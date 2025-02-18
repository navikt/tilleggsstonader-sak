package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.VirtualThreadUtil.parallelt
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val registerAktivitetService: RegisterAktivitetService,
    private val ytelseService: YtelseService,
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
                        val fagsak = fagsakMetadata[behandling.fagsakId] ?: error("Finner ikke fagsak for ${behandling.id}")
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

            val fom = stønadsperioder.minOf { it.fom }
            val tom = stønadsperioder.maxOf { it.tom }
            val registerAktiviteter = hentAktiviteter(fagsak, fom, tom)
            val registerYtelser = hentYtelser(fagsak, fom, tom)

            val alleAktiviteter = registerAktiviteter.mergeSammenhengende()
            val tiltak = registerAktiviteter.filterNot(::tiltakErUtdanning).mergeSammenhengende()
            val utdanningstiltak = registerAktiviteter.filter(::tiltakErUtdanning).mergeSammenhengende()

            val stønadsperioderSomMåKontrolleres =
                stønadsperioder
                    .map { Vedtaksperiode(it) }
                    .map { it.finnEndringer(registerYtelser, alleAktiviteter, tiltak, utdanningstiltak) }
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

    private fun Vedtaksperiode.finnEndringer(
        ytelser: Map<MålgruppeType, List<Ytelsesperiode>>,
        alleAktiviteter: List<Datoperiode>,
        tiltak: List<Datoperiode>,
        utdanningstiltak: List<Datoperiode>,
    ): StønadsperiodeForKontroll =
        StønadsperiodeForKontroll(
            fom = this.fom,
            tom = this.tom,
            målgruppe = this.målgruppe,
            aktivitet = this.aktivitet,
            endringAktivitet = finnEndringIAktivitet(tiltak, utdanningstiltak, alleAktiviteter),
            endringMålgruppe = finnEndringIMålgruppe(ytelser),
        )

    private fun Vedtaksperiode.finnEndringIMålgruppe(ytelserPerMålgruppe: Map<MålgruppeType, List<Ytelsesperiode>>): Set<ÅrsakKontroll> {
        val ytelser = ytelserPerMålgruppe[this.målgruppe] ?: emptyList()
        return finnEndring(this, ytelser)
    }

    private fun Vedtaksperiode.finnEndringIAktivitet(
        tiltak: List<Datoperiode>,
        utdanningstiltak: List<Datoperiode>,
        alleAktiviteter: List<Datoperiode>,
    ): MutableSet<ÅrsakKontroll> {
        val årsaker =
            when (this.aktivitet) {
                AktivitetType.REELL_ARBEIDSSØKER -> mutableSetOf(ÅrsakKontroll.SKAL_IKKE_KONTROLLERES)
                AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
                AktivitetType.TILTAK -> finnEndring(this, tiltak)
                AktivitetType.UTDANNING -> finnEndring(this, utdanningstiltak)
            }

        if (årsaker.any { it.trengerKontroll } && alleAktiviteter.any { it.inneholder(this) }) {
            årsaker.add(ÅrsakKontroll.TREFF_MEN_FEIL_TYPE)
        }
        return årsaker
    }

    private fun finnEndring(
        vedtaksperiode: Vedtaksperiode,
        registerperioder: List<Periode<LocalDate>>,
    ): MutableSet<ÅrsakKontroll> {
        val snitt = registerperioder.mapNotNull { vedtaksperiode.beregnSnitt(it) }
        if (snitt.isEmpty()) {
            return mutableSetOf(ÅrsakKontroll.INGEN_TREFF)
        }
        if (snitt.any { it.fom <= vedtaksperiode.fom && it.tom >= vedtaksperiode.tom }) {
            return mutableSetOf(ÅrsakKontroll.INGEN_ENDRING)
        }
        val årsaker = mutableSetOf<ÅrsakKontroll>()
        snitt.forEach {
            if (it.fom > vedtaksperiode.fom) {
                årsaker.add(ÅrsakKontroll.FOM_ENDRET)
            }
            if (it.tom < vedtaksperiode.tom) {
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

    private fun hentAktiviteter(
        fagsak: FagsakMetadata,
        fom: LocalDate,
        tom: LocalDate,
    ) = registerAktivitetService.hentAktiviteterForGrunnlagsdata(
        ident = fagsak.ident,
        fom = fom,
        tom = tom,
    )

    private fun hentYtelser(
        fagsak: FagsakMetadata,
        fom: LocalDate,
        tom: LocalDate,
    ): Map<MålgruppeType, List<Ytelsesperiode>> =
        ytelseService
            .hentYtelseForGrunnlag(
                stønadstype = fagsak.stønadstype,
                ident = fagsak.ident,
                fom = fom,
                tom = tom,
            ).perioder
            .filter { it.aapErFerdigAvklart != true }
            .filter { it.tom != null }
            .map { Ytelsesperiode(fom = it.fom, tom = it.tom!!, målgruppe = it.type.tilMålgruppe()) }
            .groupBy { it.målgruppe }
            .mapValues {
                it.value
                    .sorted()
                    .mergeSammenhengende(
                        { y1, y2 -> y1.målgruppe == y2.målgruppe && y1.overlapperEllerPåfølgesAv(y2) },
                        { y1, y2 -> y1.copy(fom = minOf(y1.fom, y2.fom), tom = maxOf(y2.tom, y2.tom)) },
                    )
            }

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

    private fun TypeYtelsePeriode.tilMålgruppe() =
        when (this) {
            TypeYtelsePeriode.AAP -> MålgruppeType.AAP
            TypeYtelsePeriode.ENSLIG_FORSØRGER -> MålgruppeType.OVERGANGSSTØNAD
            TypeYtelsePeriode.OMSTILLINGSSTØNAD -> MålgruppeType.OMSTILLINGSSTØNAD
        }

    private data class Ytelsesperiode(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: MålgruppeType,
    ) : Periode<LocalDate>

    /**
     * Foreløpig mappes stønadsperiode til vedtaksperiode for å sjekke om det er diff mot stønadsperiode
     */
    private data class Vedtaksperiode(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val målgruppe: MålgruppeType,
        val aktivitet: AktivitetType,
    ) : Periode<LocalDate>,
        KopierPeriode<Vedtaksperiode> {
        constructor(stønadsperiode: StønadsperiodeDto) : this(
            fom = stønadsperiode.fom,
            tom = stønadsperiode.tom,
            målgruppe = stønadsperiode.målgruppe,
            aktivitet = stønadsperiode.aktivitet,
        )

        override fun medPeriode(
            fom: LocalDate,
            tom: LocalDate,
        ): Vedtaksperiode = this.copy(fom = fom, tom = tom)
    }
}
