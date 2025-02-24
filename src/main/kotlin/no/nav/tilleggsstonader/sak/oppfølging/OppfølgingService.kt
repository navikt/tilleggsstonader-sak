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
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaProsent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val stønadsperiodeService: StønadsperiodeService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
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

    fun opprettOppfølging(behandlingId: BehandlingId): Oppfølging? {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsakMetadata = fagsakService.hentMetadata(listOf(behandling.fagsakId)).values.single()

        val perioderForKontroll = hentPerioderForKontroll(behandling, fagsakMetadata)
        if (perioderForKontroll.isNotEmpty()) {
            val data = OppfølgingData(perioderTilKontroll = perioderForKontroll)
            val sisteForFagsak = oppfølgingRepository.finnSisteForFagsak(behandlingId)
            if (sisteForFagsak?.kontrollert?.utfall != KontrollertUtfall.IGNORERES || sisteForFagsak.data != data) {
                return oppfølgingRepository.insert(Oppfølging(behandlingId = behandlingId, data = data))
            } else {
                logger.warn(
                    "Ingen endring for behandling=$behandlingId siden oppfølging=${sisteForFagsak.id} " +
                        "ble kontrollert forrige gang, oppretter ikke ny oppfølging",
                )
            }
        }
        return null
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
        val aktiviteter = hentInngangsvilkårAktiviteter(behandling, registerAktiviteter)

        return stønadsperioder
            .map { Vedtaksperiode(it) }
            .map { it.finnEndringer(registerYtelser, registerAktiviteter, aktiviteter) }
            .filter { it.trengerKontroll() }
    }

    private fun Vedtaksperiode.finnEndringer(
        registerYtelser: Map<MålgruppeType, List<Ytelsesperiode>>,
        registerAktiviteter: RegisterAktiviteter,
        aktiviteter: List<InngangsvilkårAktivitet>,
    ): PeriodeForKontroll =
        PeriodeForKontroll(
            fom = this.fom,
            tom = this.tom,
            målgruppe = this.målgruppe,
            aktivitet = this.aktivitet,
            endringAktivitet = finnEndringIAktivitet(registerAktiviteter, aktiviteter),
            endringMålgruppe = finnEndringIMålgruppe(registerYtelser),
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
                val førsteDagINestNesteMåned = YearMonth.now().plusMonths(1).atEndOfMonth()
                if (
                    målgruppe == MålgruppeType.AAP &&
                    enKontroll?.årsak == ÅrsakKontroll.TOM_ENDRET &&
                    enKontroll.tom!! >= førsteDagINestNesteMåned
                ) {
                    // AAP slutter før vedtaksperiode
                    emptyList()
                } else {
                    kontroller
                }
            }

            else -> emptyList() // Sjekker kun målgrupper som vi henter fra andre systemer
        }

    private fun Vedtaksperiode.finnEndringIAktivitet(
        registerAktiviteter: RegisterAktiviteter,
        aktiviteter: List<InngangsvilkårAktivitet>,
    ): List<Kontroll> {
        val kontroller =
            when (this.aktivitet) {
                AktivitetType.REELL_ARBEIDSSØKER -> mutableListOf() // Skal ikke kontrolleres
                AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
                AktivitetType.TILTAK ->
                    finnEndringIRegisteraktivitetEllerAlle(this, aktiviteter, registerAktiviteter.tiltak)

                AktivitetType.UTDANNING ->
                    finnEndringIRegisteraktivitetEllerAlle(this, aktiviteter, registerAktiviteter.utdanningstiltak)
            }

        val ingenTreff = kontroller.any { it.årsak == ÅrsakKontroll.INGEN_TREFF }
        if (ingenTreff && registerAktiviteter.alleAktiviteter.any { it.inneholder(this) }) {
            return kontroller + Kontroll(ÅrsakKontroll.TREFF_MEN_FEIL_TYPE)
        }
        return kontroller
    }

    /**
     * Kontrollerer om det er endringer i registeraktivitet
     * * Kontrollerer om en registeraktivitet inneholder hele vedtaksperioden
     * * Kontrollerer om
     */
    private fun finnEndringIRegisteraktivitetEllerAlle(
        vedtaksperiode: Vedtaksperiode,
        aktiviteter: List<InngangsvilkårAktivitet>,
        registerperioder: List<Periode<LocalDate>>,
    ): List<Kontroll> {
        val kontroller = mutableListOf<Kontroll>()

        aktiviteter.forEach { aktivitet ->
            val register = aktivitet.datoperiodeAktivitet
            if (register != null && register.inneholder(vedtaksperiode)) {
                // Har overlapp mellom register-data og vedtaksperiode
                // returnerer tom liste for finnKontrollerAktivitet
                return mutableListOf()
            }
            kontroller.addAll(kontrollerEndringerMotRegisterAktivitet(vedtaksperiode, aktivitet, register))
        }
        val kontrollMotAlleRegisterperioder = finnKontroller(vedtaksperiode, registerperioder)
        kontroller.addAll(kontrollMotAlleRegisterperioder)
        return kontroller.distinct()
    }

    /**
     * Kontrollerer om endring i registeraktivitet påvirker snittet av en [Vedtaksperiode] og [InngangsvilkårAktivitet]
     * En registeraktivitet kan ha endret seg, men det er ikke sikkert endringen påvirker vedtaksperioden
     * Hvis man har flere aktiviteter som løper parallellt og en av de
     *
     * @param registerperiode er trukket ut fra [aktivitet] men er not null
     */
    private fun kontrollerEndringerMotRegisterAktivitet(
        vedtaksperiode: Vedtaksperiode,
        aktivitet: InngangsvilkårAktivitet,
        registerperiode: Datoperiode?,
    ): List<Kontroll> {
        val snitt = vedtaksperiode.beregnSnitt(aktivitet)
        if (registerperiode != null && snitt != null) {
            // Kontrollerer om registeraktiviteten endret seg mott snittet av vedtaksperioden og aktiviteten
            return finnEndringFomTom(snitt, registerperiode)
        }
        val finnerSnittMenManglerRegisteraktivitet =
            registerperiode == null && snitt != null && aktivitet.kildeId != null
        if (finnerSnittMenManglerRegisteraktivitet) {
            return listOf(Kontroll(ÅrsakKontroll.FINNER_IKKE_REGISTERAKTIVITET))
        }
        return emptyList()
    }

    private fun finnKontroller(
        vedtaksperiode: Vedtaksperiode,
        registerperioder: List<Periode<LocalDate>>,
    ): List<Kontroll> {
        val snitt = registerperioder.mapNotNull { vedtaksperiode.beregnSnitt(it) }.singleOrNull()

        if (snitt == null) {
            return listOf(Kontroll(ÅrsakKontroll.INGEN_TREFF))
        }
        if (snitt.fom == vedtaksperiode.fom && snitt.tom == vedtaksperiode.tom) {
            return emptyList() // Snitt er lik vedtaksperiode -> skal ikke kontrolleres
        }
        return finnEndringFomTom(vedtaksperiode, snitt)
    }

    private fun finnEndringFomTom(
        vedtaksperiode: Vedtaksperiode,
        register: Periode<LocalDate>,
    ): MutableList<Kontroll> =
        mutableListOf<Kontroll>().apply {
            if (register.fom > vedtaksperiode.fom) {
                add(Kontroll(ÅrsakKontroll.FOM_ENDRET, fom = register.fom))
            }
            if (register.tom < vedtaksperiode.tom) {
                add(Kontroll(ÅrsakKontroll.TOM_ENDRET, tom = register.tom))
            }
        }

    private fun hentInngangsvilkårAktiviteter(
        behandling: Behandling,
        registerAktiviteter: RegisterAktiviteter,
    ): List<InngangsvilkårAktivitet> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandling.id, ResultatVilkårperiode.OPPFYLT)
            .ofType<AktivitetFaktaOgVurdering>()
            .map { vilkår -> InngangsvilkårAktivitet(vilkår, registerAktiviteter.forId(vilkår.kildeId)) }
            .sorted()

    private fun hentAktiviteter(
        fagsak: FagsakMetadata,
        fom: LocalDate,
        tom: LocalDate,
    ) = registerAktivitetService
        .hentAktiviteterForGrunnlagsdata(
            ident = fagsak.ident,
            fom = fom,
            tom = tom,
        ).let { RegisterAktiviteter(it) }

    private fun hentYtelser(
        fagsak: FagsakMetadata,
        fom: LocalDate,
        tom: LocalDate,
    ): Map<MålgruppeType, List<Ytelsesperiode>> {
        val ytelseForGrunnlag =
            ytelseService
                .hentYtelseForGrunnlag(
                    stønadstype = fagsak.stønadstype,
                    ident = fagsak.ident,
                    fom = fom,
                    tom = tom,
                )
        val hentetInformasjon = ytelseForGrunnlag.hentetInformasjon.filter { it.status != StatusHentetInformasjon.OK }
        if (hentetInformasjon.isNotEmpty()) {
            error("Feilet henting av ytelser=${hentetInformasjon.map { it.type }}")
        }
        return ytelseForGrunnlag.perioder
            .filter { it.aapErFerdigAvklart != true }
            .filter { it.tom != null }
            .map { Ytelsesperiode(fom = it.fom, tom = it.tom!!, målgruppe = it.type.tilMålgruppe()) }
            .groupBy { it.målgruppe }
            .mapValues {
                it.value
                    .sorted()
                    .mergeSammenhengende(
                        { y1, y2 -> y1.målgruppe == y2.målgruppe && y1.overlapperEllerPåfølgesAv(y2) },
                        { y1, y2 -> y1.copy(fom = minOf(y1.fom, y2.fom), tom = maxOf(y1.tom, y2.tom)) },
                    )
            }
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

private data class InngangsvilkårAktivitet(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val aktivitet: AktivitetType,
    val prosent: Int?,
    val antallDager: Int?,
    val kildeId: String?,
    val registerAktivitet: AktivitetArenaDto?,
) : Periode<LocalDate> {
    init {
        fun <T> diff(
            type: String,
            vilkårVerdi: T,
            registerVerdi: T,
        ): String? =
            if (vilkårVerdi != registerVerdi) {
                "$type=$vilkårVerdi ${type}Register=$registerVerdi"
            } else {
                null
            }
        if (registerAktivitet != null) {
            val fomDiff = diff("fom", fom, registerAktivitet.fom)
            val tomDiff = diff("tom", tom, registerAktivitet.tom)
            val prosentDiff = diff("prosent", prosent, registerAktivitet.prosentDeltakelse?.toInt())
            val antallDagerDiff = diff("dager", antallDager, registerAktivitet.antallDagerPerUke)
            val diff = listOfNotNull(fomDiff, tomDiff, prosentDiff, antallDagerDiff)
            if (diff.isNotEmpty()) {
                logger.info("Diff vilkår vs register ${diff.joinToString(" ")}")
            }
        }
    }

    val datoperiodeAktivitet: Datoperiode? by lazy {
        val fom = registerAktivitet?.fom
        val tom = registerAktivitet?.tom
        if (fom != null && tom != null) {
            Datoperiode(fom = fom, tom = tom)
        } else {
            null
        }
    }

    constructor(
        vilkårperiode: GeneriskVilkårperiode<AktivitetFaktaOgVurdering>,
        registerAktivitet: AktivitetArenaDto?,
    ) : this(
        id = vilkårperiode.id,
        fom = vilkårperiode.fom,
        tom = vilkårperiode.tom,
        aktivitet = vilkårperiode.faktaOgVurdering.type.vilkårperiodeType,
        prosent =
            vilkårperiode.faktaOgVurdering.fakta
                .takeIfFakta<FaktaProsent>()
                ?.prosent,
        antallDager =
            vilkårperiode.faktaOgVurdering.fakta
                .takeIfFakta<FaktaAktivitetsdager>()
                ?.aktivitetsdager,
        kildeId = vilkårperiode.kildeId,
        registerAktivitet = registerAktivitet,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(InngangsvilkårAktivitet::class.java)
    }
}

private class RegisterAktiviteter(
    private val aktiviteter: List<AktivitetArenaDto>,
) {
    private val aktivitetPåId: Map<String, AktivitetArenaDto> by lazy {
        aktiviteter.associateBy { it.id }
    }

    val alleAktiviteter: List<Datoperiode> by lazy {
        aktiviteter.mergeSammenhengende()
    }
    val tiltak: List<Datoperiode> by lazy {
        aktiviteter.filterNot(::tiltakErUtdanning).mergeSammenhengende()
    }
    val utdanningstiltak: List<Datoperiode> by lazy {
        aktiviteter.filter(::tiltakErUtdanning).mergeSammenhengende()
    }

    fun forId(id: String?) = id?.let { aktivitetPåId[it] }

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

    companion object {
        private val logger = LoggerFactory.getLogger(RegisterAktiviteter::class.java)
    }
}
