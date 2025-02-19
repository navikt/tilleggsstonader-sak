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
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val registerAktivitetService: RegisterAktivitetService,
    private val ytelseService: YtelseService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val oppfølgingRepository: OppfølgingRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Oppretter en task med unik behandlingId og tidspunkt
     */
    @Transactional
    fun opprettTaskerForOppfølging() {
        val tidspunkt = LocalDateTime.now()
        oppfølgingRepository.markerAlleAktiveSomIkkeAktive()
        Stønadstype.entries.forEach { stønadstype ->
            val behandlinger = behandlingRepository.finnGjeldendeIverksatteBehandlinger(stønadstype = stønadstype)
            taskService.saveAll(behandlinger.map { OppfølgingTask.opprettTask(it.id, tidspunkt) })
        }
    }

    fun kontroller(request: KontrollerOppfølgingRequest): OppfølgingMedDetaljer {
        val oppfølging = oppfølgingRepository.findByIdOrThrow(request.id)
        brukerfeilHvis(oppfølging.version != request.version) {
            "Det har allerede skjedd en endring på oppfølging. Last siden på nytt"
        }
        val kontrollert =
            Kontrollert(
                utfall = request.utfall,
                kommentar = request.kommentar,
            )
        oppfølgingRepository.update(oppfølging.copy(kontrollert = kontrollert))
        return oppfølgingRepository.finnAktivMedDetaljer(oppfølging.behandlingId)
    }

    fun hentAktiveOppfølginger(): List<OppfølgingMedDetaljer> =
        oppfølgingRepository
            .finnAktiveMedDetaljer()
            .sortedBy { it.behandlingsdetaljer.vedtakstidspunkt }

    fun opprettOppfølging(behandlingId: BehandlingId) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsakMetadata = fagsakService.hentMetadata(listOf(behandling.fagsakId)).values.single()
        val perioderForKontroll = hentPerioderForKontroll(behandling, fagsakMetadata)
        if (perioderForKontroll.isNotEmpty()) {
            val data = OppfølgingData(perioderTilKontroll = perioderForKontroll)
            oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))
        }
    }

    private fun hentPerioderForKontroll(
        behandling: Behandling,
        fagsak: FagsakMetadata,
    ): List<PeriodeForKontroll> {
        val stønadsperioder = stønadsperiodeService.hentStønadsperioder(behandling.id)

        val fom = stønadsperioder.minOf { it.fom }
        val tom = stønadsperioder.maxOf { it.tom }
        val registerAktiviteter = hentAktiviteter(fagsak, fom, tom)
        val registerYtelser = hentYtelser(fagsak, fom, tom)

        val alleAktiviteter = registerAktiviteter.mergeSammenhengende()
        val tiltak = registerAktiviteter.filterNot(::tiltakErUtdanning).mergeSammenhengende()
        val utdanningstiltak = registerAktiviteter.filter(::tiltakErUtdanning).mergeSammenhengende()

        return stønadsperioder
            .map { Vedtaksperiode(it) }
            .map { it.finnEndringer(registerYtelser, alleAktiviteter, tiltak, utdanningstiltak) }
            .filter { it.trengerKontroll() }
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

    private fun Vedtaksperiode.finnEndringIMålgruppe(ytelserPerMålgruppe: Map<MålgruppeType, List<Ytelsesperiode>>): List<Kontroll> =
        when (this.målgruppe) {
            MålgruppeType.AAP,
            MålgruppeType.OMSTILLINGSSTØNAD,
            MålgruppeType.OVERGANGSSTØNAD,
            -> {
                val ytelser = ytelserPerMålgruppe[this.målgruppe] ?: emptyList()
                val kontroller = finnKontroller(this, ytelser)
                val enKontroll = kontroller.singleOrNull()
                val sisteDagNesteMåned = YearMonth.now().plusMonths(1).atEndOfMonth()
                if (enKontroll?.årsak == ÅrsakKontroll.TOM_ENDRET && enKontroll.tom!! > sisteDagNesteMåned) {
                    listOf(Kontroll(ÅrsakKontroll.AAP_SLUTTER_FØR_VEDTAKSPERIODE))
                } else {
                    kontroller
                }
            }
            else -> listOf(Kontroll(ÅrsakKontroll.SKAL_IKKE_KONTROLLERES))
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
