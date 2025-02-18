package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.util.VirtualThreadUtil.parallelt
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val registerAktivitetService: RegisterAktivitetService,
    private val fagsakService: FagsakService,
) {
    val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Hent alle behandlinger som er iverksatt og sjekk om det er stønadsperioder som må kontrolleres.
     *
     * Hent aktiviteter og sjekk om det er aktiviteter som overlapper stønadsperiodene.
     *
     * Hent ytelsesperioder og gjør det samme
     *
     * Ignorer aktivitetestypene, og målgruppene som vi ikke henter. (Reel arbeidssøker)
     *
     * Henter hver chunk parallellt for å redusere tiden
     */
    fun hentBehandlingerForOppfølging(): List<BehandlingForOppfølgingDto> {
        val behandlingerMedMuligLøpendePerioder = behandlingRepository.finnGjeldendeIverksatteBehandlinger()
        val fagsakMetadata = fagsakService.hentMetadata(behandlingerMedMuligLøpendePerioder.map { it.fagsakId })

        return behandlingerMedMuligLøpendePerioder
            .chunked(5)
            .flatMap { chunk ->
                chunk
                    .map { behandling ->
                        val fagsak =
                            fagsakMetadata[behandling.fagsakId] ?: error("Finner ikke fagsak for ${behandling.id}")
                        hentOppfølgningFn(behandling, fagsak)
                    }.parallelt()
            }.mapNotNull { it }
    }

    /**
     * Svarer med en lambda for å kunne parallellisere en chunk i [hentBehandlingerForOppfølging]
     */
    private fun hentOppfølgningFn(
        behandling: Behandling,
        fagsak: FagsakMetadata,
    ): () -> BehandlingForOppfølgingDto? =
        {
            val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandling.id)

            val registerAktiviteter =
                registerAktivitetService.hentAktiviteterForGrunnlagsdata(
                    fagsak.ident,
                    stønadsperioder.minOf { it.fom },
                    stønadsperioder.maxOf { it.tom },
                )
            val alleAktiviteter = registerAktiviteter.mergeSammenhengende()
            val tiltak = registerAktiviteter.filterNot(::tiltakErUtdanning).mergeSammenhengende()
            val utdanningstiltak = registerAktiviteter.filter(::tiltakErUtdanning).mergeSammenhengende()

            val stønadsperioderSomMåKontrolleres =
                stønadsperioder
                    .map { it.finnEndringer(alleAktiviteter, tiltak, utdanningstiltak) }
                    .filter { it.trengerKontroll() }

            if (stønadsperioderSomMåKontrolleres.isNotEmpty()) {
                BehandlingForOppfølgingDto(
                    behandling = tilBehandlingsinformasjon(behandling, fagsak),
                    stønadsperioderForKontroll = stønadsperioderSomMåKontrolleres,
                    registerAktiviteter = registerAktiviteter.tilDto(),
                )
            } else {
                null
            }
        }

    private fun StønadsperiodeDto.finnEndringer(
        alleAktiviteter: List<Datoperiode>,
        tiltak: List<Datoperiode>,
        utdanningstiltak: List<Datoperiode>,
    ): StønadsperiodeForKontroll {
        val stønadsperiode = Datoperiode(fom = this.fom, tom = this.tom)
        val årsaker =
            when (this.aktivitet) {
                AktivitetType.REELL_ARBEIDSSØKER -> mutableSetOf(ÅrsakKontroll.SKAL_IKKE_KONTROLLERES)
                AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
                AktivitetType.TILTAK -> finnEndring(stønadsperiode, tiltak)
                AktivitetType.UTDANNING -> finnEndring(stønadsperiode, utdanningstiltak)
            }

        if (årsaker.any { it.trengerKontroll } && alleAktiviteter.any { it.inneholder(stønadsperiode) }) {
            årsaker.add(ÅrsakKontroll.TREFF_MEN_FEIL_TYPE)
        }
        return StønadsperiodeForKontroll(
            fom = this.fom,
            tom = this.tom,
            målgruppe = this.målgruppe,
            aktivitet = this.aktivitet,
            årsaker = årsaker,
        )
    }

    private fun finnEndring(
        stønadsperiode: Datoperiode,
        tiltak: List<Datoperiode>,
    ): MutableSet<ÅrsakKontroll> {
        val snitt = tiltak.mapNotNull { it.beregnSnitt(stønadsperiode) }
        if (snitt.isEmpty()) {
            return mutableSetOf(ÅrsakKontroll.INGEN_TREFF)
        }
        if (snitt.any { it.fom <= stønadsperiode.fom && it.tom >= stønadsperiode.tom }) {
            return mutableSetOf(ÅrsakKontroll.INGEN_ENDRING)
        }
        val årsaker = mutableSetOf<ÅrsakKontroll>()
        snitt.forEach {
            if (it.fom > stønadsperiode.fom) {
                årsaker.add(ÅrsakKontroll.FOM_ENDRET)
            }
            if (it.tom < stønadsperiode.tom) {
                årsaker.add(ÅrsakKontroll.TOM_ENDRET)
            }
        }
        return årsaker
    }

    private fun List<AktivitetArenaDto>.mergeSammenhengende() =
        this
            .mapNotNull { mapTilPeriode(it) }
            .sorted()
            .mergeSammenhengende { a, b -> a.overlapper(b) || a.påfølgesAv(b) }

    private fun mapTilPeriode(aktivitet: AktivitetArenaDto): Datoperiode? {
        if (aktivitet.fom == null || aktivitet.tom == null) {
            logger.warn("Aktivitet med id=${aktivitet.id} mangler fom eller tom dato: ${aktivitet.fom} - ${aktivitet.tom}")
            return null
        }
        return Datoperiode(aktivitet.fom!!, aktivitet.tom!!)
    }

    private fun tiltakErUtdanning(it: AktivitetArenaDto) = it.erUtdanning ?: false

    private fun tilBehandlingsinformasjon(
        behandling: Behandling,
        fagsak: FagsakMetadata,
    ) = BehandlingInformasjon(
        behandlingId = behandling.id,
        fagsakId = fagsak.id,
        eksternFagsakId = fagsak.eksternFagsakId,
        stønadstype = fagsak.stønadstype,
        vedtakstidspunkt = behandling.vedtakstidspunkt ?: error("Behandling=${behandling.id} mangler vedtakstidspunkt"),
    )

    private fun List<AktivitetArenaDto>.tilDto() =
        map {
            RegisterAktivitetDto(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                typeNavn = it.typeNavn,
                status = it.status,
                erUtdanning = it.erUtdanning,
            )
        }
}
