package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.VirtualThreadUtil.parallelt
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val registerAktivitetService: RegisterAktivitetService,
    private val ytelseService: YtelseService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val oppfølgningRepository: OppfølgningRepository,
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
     * Oppretter en task med unik behandlingId og tidspunkt
     */
    @Transactional
    fun opprettTaskerForOppfølging() {
        val tidspunkt = LocalDateTime.now()
        oppfølgningRepository.markerAlleAktiveSomIkkeAktive()
        Stønadstype.entries.forEach { stønadstype ->
            val behandlinger = behandlingRepository.finnGjeldendeIverksatteBehandlinger(stønadstype = stønadstype)
            taskService.saveAll(behandlinger.map { OppfølgningTask.opprettTask(it.id, tidspunkt) })
        }
    }

    fun kontroller(request: KontrollerOppfølgingRequest) {
        val oppfølging = oppfølgningRepository.findByIdOrThrow(request.id)
        brukerfeilHvis(oppfølging.version != request.version) {
            "Det har allerede skjedd en endring på oppfølging. Last siden på nytt"
        }
        val kontrollert =
            Kontrollert(
                utfall = request.utfall,
                kommentar = request.kommentar,
            )
        oppfølgningRepository.update(oppfølging.copy(kontrollert = kontrollert))
    }

    fun hentAktiveOppfølginger(): List<OppfølgingMedDetaljer> = oppfølgningRepository.finnAktiveMedDetaljer()

    fun håndterBehandling(behandlingId: BehandlingId) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsakMetadata = fagsakService.hentMetadata(listOf(behandling.fagsakId)).values.single()
        hentOppfølgningFn(behandling, fagsakMetadata)()?.let {
            val data =
                OppfølgingData(
                    stønadstype = fagsakMetadata.stønadstype,
                    vedtakstidspunkt = behandling.vedtakstidspunkt!!,
                    perioderTilKontroll = it.stønadsperioderForKontroll,
                )
            oppfølgningRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))
        }
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
    ): PeriodeForKontroll =
        PeriodeForKontroll(
            fom = this.fom,
            tom = this.tom,
            målgruppe = this.målgruppe,
            aktivitet = this.aktivitet,
            endringAktivitet = finnEndringIAktivitet(tiltak, utdanningstiltak, alleAktiviteter),
            endringMålgruppe = finnEndringIMålgruppe(ytelser),
        )

    private fun Vedtaksperiode.finnEndringIMålgruppe(ytelserPerMålgruppe: Map<MålgruppeType, List<Ytelsesperiode>>): List<Kontroll> {
        val ytelser = ytelserPerMålgruppe[this.målgruppe] ?: emptyList()
        return finnKontroller(this, ytelser)
    }

    private fun Vedtaksperiode.finnEndringIAktivitet(
        tiltak: List<Datoperiode>,
        utdanningstiltak: List<Datoperiode>,
        alleAktiviteter: List<Datoperiode>,
    ): List<Kontroll> {
        val kontroller =
            when (this.aktivitet) {
                AktivitetType.REELL_ARBEIDSSØKER -> mutableListOf(Kontroll(ÅrsakKontroll.SKAL_IKKE_KONTROLLERES))
                AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
                AktivitetType.TILTAK -> finnKontroller(this, tiltak)
                AktivitetType.UTDANNING -> finnKontroller(this, utdanningstiltak)
            }

        if (kontroller.any { it.årsak.trengerKontroll } && alleAktiviteter.any { it.inneholder(this) }) {
            kontroller.add(Kontroll(ÅrsakKontroll.TREFF_MEN_FEIL_TYPE))
        }
        return kontroller
    }

    private fun finnKontroller(
        vedtaksperiode: Vedtaksperiode,
        registerperioder: List<Periode<LocalDate>>,
    ): MutableList<Kontroll> {
        // har 1 eller inget snitt
        val snitt = registerperioder.mapNotNull { vedtaksperiode.beregnSnitt(it) }.singleOrNull()

        if (snitt == null) {
            return mutableListOf(Kontroll(ÅrsakKontroll.INGEN_TREFF))
        }
        if (snitt.fom == vedtaksperiode.fom && snitt.tom == vedtaksperiode.tom) {
            return mutableListOf(Kontroll(ÅrsakKontroll.INGEN_ENDRING))
        }
        return mutableListOf<Kontroll>().apply {
            if (snitt.fom > vedtaksperiode.fom) {
                add(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = snitt.fom))
            }
            if (snitt.tom < vedtaksperiode.tom) {
                add(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = snitt.tom))
            }
        }
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
        this
            .filter { it.fom == null || it.tom == null }
            .map {
                RegisterAktivitetDto(
                    id = it.id,
                    fom = it.fom!!,
                    tom = it.tom!!,
                    typeNavn = it.typeNavn,
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
