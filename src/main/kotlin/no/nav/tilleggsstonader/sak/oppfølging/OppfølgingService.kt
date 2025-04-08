package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
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
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFakta
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaProsent
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeFaktaOgVurdering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

/**
 * Vi har nå oppfølginger som går på vedtaksperiode som inneholder målgruppe og aktivitet
 * Men så har vi endret til at man har faktisk målgruppe på vedtaksperioden
 *
 * Alternativer:
 *  * Ikke utgå fra vedtaksperiode
 *  * Kontrollere målgruppe/aktivitet uten å forholde seg til vedtaksperiode
 *    * Hvis det er endring, sjekk om det påvirker vedtaksperioden
 *  * Endre oppfølging-kontroll til å være per type, dvs aktivitet/målgruppe med endring
 *   * Må håndtere tidligere oppfølginger for å ikke behandle ignorerte på nytt
 */
@Service
class OppfølgingService(
    private val behandlingRepository: BehandlingRepository,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vedtakRepository: VedtakRepository,
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
        feilHvis(!oppfølging.aktiv) {
            "Kan ikke redigere en oppfølging som ikke lengre er aktiv"
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
        val vedtaksperioder = hentVedtaksperioder(fagsak, behandling)
        if (vedtaksperioder.isEmpty()) {
            return emptyList()
        }

        val fom = vedtaksperioder.minOf { it.fom }
        val tom = vedtaksperioder.maxOf { it.tom }
        val registerAktiviteter = hentAktiviteter(fagsak, fom, tom)
        val inngangsvilkår = hentInngangsvilkår(behandling, registerAktiviteter)
        val registerYtelser = hentYtelser(fagsak, inngangsvilkår.målgrupper)

        val kontrollerAktivitet = vedtaksperioder.map { it.finnEndringer(registerAktiviteter, inngangsvilkår) }
        val kontrollerMålgrupper = finnKontrollerMålgrupper(vedtaksperioder, inngangsvilkår, registerYtelser)
        return (kontrollerAktivitet + kontrollerMålgrupper).filter { it.trengerKontroll() }
    }

    private fun finnKontrollerMålgrupper(
        vedtaksperiodes: List<Vedtaksperiode>,
        vilkårperioder: Vilkårperioder,
        registerytelser: Map<MålgruppeType, List<Datoperiode>>,
    ): List<PeriodeForKontroll> =
        vilkårperioder.målgrupper
            .filter { it.skalKontrolleres() }
            .map {
                val endringer =
                    it
                        .finnEndringer(vedtaksperiodes, registerytelser)
                        .utenAAPSomGjelderFraOgMedNesteMåned(målgruppe = it.målgruppe)

                PeriodeForKontroll(
                    fom = it.fom,
                    tom = it.tom,
                    type = it.målgruppe,
                    endringer = endringer,
                )
            }

    private fun hentVedtaksperioder(
        fagsak: FagsakMetadata,
        behandling: Behandling,
    ): List<Vedtaksperiode> {
        val vedtaksperioder =
            when (fagsak.stønadstype) {
                Stønadstype.BARNETILSYN ->
                    hentVedtak<InnvilgelseEllerOpphørTilsynBarn>(behandling)
                        .vedtaksperioder
                        ?: emptyList()

                Stønadstype.LÆREMIDLER ->
                    hentVedtak<InnvilgelseEllerOpphørLæremidler>(behandling)
                        .vedtaksperioder
                        .tilFellesFormat()

                Stønadstype.BOUTGIFTER ->
                    hentVedtak<InnvilgelseEllerOpphørBoutgifter>(behandling)
                        .vedtaksperioder
                        ?: emptyList()
            }
        return vedtaksperioder
            .sorted()
            .mergeSammenhengende(
                { v1, v2 ->
                    v1.overlapperEllerPåfølgesAv(v2) &&
                        v1.målgruppe == v2.målgruppe &&
                        v1.aktivitet == v2.aktivitet
                },
                { v1, v2 -> v1.medPeriode(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
            )
    }

    private inline fun <reified T : Vedtaksdata> hentVedtak(behandling: Behandling) =
        vedtakRepository.findByIdOrThrow(behandling.id).withTypeOrThrow<T>().data

    private fun Vedtaksperiode.finnEndringer(
        registerAktiviteter: RegisterAktiviteter,
        inngangsvilkår: Vilkårperioder,
    ): PeriodeForKontroll {
        val endringerAktivitet = finnEndringIAktivitet(registerAktiviteter, inngangsvilkår.aktiviteter)
        return PeriodeForKontroll(
            fom = this.fom,
            tom = this.tom,
            type = this.aktivitet,
            endringer = endringerAktivitet,
        )
    }

    /**
     * Ønsker å finne om et inngangsvilkår fortsatt er gyldig
     * Om snittet av inngangsvilkåret og registerinfo er kortere enn inngangsvilkåret og om dette påvirker vedtaksperioden
     *
     * Hvis et inngangsvilkår slutter før en vedtaksperiode, er det kun snittet av disse som er interessant
     */
    @Suppress("SimplifiableCallChain")
    private fun InngangsvilkårMålgruppe.finnEndringer(
        vedtaksperioder: List<Vedtaksperiode>,
        ytelserPerMålgruppe: Map<MålgruppeType, List<Datoperiode>>,
    ): List<Kontroll> {
        val snittInngangsvilkårVedtaksperiode =
            vedtaksperioder
                .filter { it.målgruppe == this.målgruppe.faktiskMålgruppe() }
                .mapNotNull { it.beregnSnitt(this) }

        val ytelserFraRegister = ytelserPerMålgruppe[this.målgruppe] ?: emptyList()

        return snittInngangsvilkårVedtaksperiode.flatMap { vedtaksperiode ->
            val snitt = ytelserFraRegister.mapNotNull { it.beregnSnitt(vedtaksperiode) }.firstOrNull()
            if (snitt == null) {
                listOf(Kontroll(ÅrsakKontroll.INGEN_TREFF))
            } else {
                this@OppfølgingService.finnEndringFomTom(vedtaksperiode, snitt)
            }
        }
    }

    // Filtrerer vekk kontroller hvis det kun gjelder AAP som påvirkes etter neste måned
    private fun List<Kontroll>.utenAAPSomGjelderFraOgMedNesteMåned(målgruppe: MålgruppeType): List<Kontroll> {
        val førsteDagINestNesteMåned = YearMonth.now().plusMonths(1).atEndOfMonth()
        return this.filterNot {
            målgruppe == MålgruppeType.AAP &&
                it.årsak == ÅrsakKontroll.TOM_ENDRET &&
                it.tom!! >= førsteDagINestNesteMåned
        }
    }

    private fun InngangsvilkårMålgruppe.skalKontrolleres() =
        when (this.målgruppe) {
            MålgruppeType.AAP,
            MålgruppeType.OMSTILLINGSSTØNAD,
            MålgruppeType.OVERGANGSSTØNAD,
            -> true

            MålgruppeType.DAGPENGER -> error("Håndterer ikke dagpenger ennå")
            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE,
            -> false
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

    private fun hentInngangsvilkår(
        behandling: Behandling,
        registerAktiviteter: RegisterAktiviteter,
    ): Vilkårperioder {
        val vilkårperioder =
            vilkårperiodeRepository.findByBehandlingIdAndResultat(behandling.id, ResultatVilkårperiode.OPPFYLT)

        val aktiviteter =
            vilkårperioder
                .ofType<AktivitetFaktaOgVurdering>()
                .map { vilkår -> InngangsvilkårAktivitet(vilkår, registerAktiviteter.forId(vilkår.kildeId)) }
                .sorted()
        val målgrupper =
            vilkårperioder
                .ofType<MålgruppeFaktaOgVurdering>()
                .map { InngangsvilkårMålgruppe(it) }
                .sorted()
                .mergeSammenhengende({ m1, m2 -> m1.overlapperEllerPåfølgesAv(m2) && m1.målgruppe == m2.målgruppe })
        return Vilkårperioder(
            målgrupper = målgrupper,
            aktiviteter = aktiviteter,
        )
    }

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
        målgrupper: List<InngangsvilkårMålgruppe>,
    ): Map<MålgruppeType, List<Datoperiode>> {
        val typerSomSkalHentes =
            målgrupper
                .mapNotNull { periode ->
                    periode.målgruppe.tilTypeYtelsePeriode()?.let { it to periode }
                }
        if (typerSomSkalHentes.isEmpty()) {
            return emptyMap()
        }
        val typer = typerSomSkalHentes.map { it.first }.distinct()
        val fom = typerSomSkalHentes.minOf { it.second.fom }
        val tom = typerSomSkalHentes.maxOf { it.second.tom }
        val ytelseForGrunnlag = ytelseService.hentYtelser(fagsak.ident, fom = fom, tom = tom, typer)
        val hentetInformasjon = ytelseForGrunnlag.hentetInformasjon.filter { it.status != StatusHentetInformasjon.OK }
        if (hentetInformasjon.isNotEmpty()) {
            error("Feilet henting av ytelser=${hentetInformasjon.map { it.type }}")
        }
        return ytelseForGrunnlag.perioder
            .filter { it.aapErFerdigAvklart != true }
            .filter { it.tom != null }
            .map { it.type.tilMålgruppe() to Datoperiode(fom = it.fom, tom = it.tom!!) }
            .groupBy { it.first }
            .mapValues {
                it.value
                    .map { it.second }
                    .sorted()
                    .mergeSammenhengende { y1, y2 -> y1.overlapperEllerPåfølgesAv(y2) }
                    .map { Datoperiode(fom = it.fom, tom = it.tom) }
            }
    }

    private fun TypeYtelsePeriode.tilMålgruppe() =
        when (this) {
            TypeYtelsePeriode.AAP -> MålgruppeType.AAP
            TypeYtelsePeriode.ENSLIG_FORSØRGER -> MålgruppeType.OVERGANGSSTØNAD
            TypeYtelsePeriode.OMSTILLINGSSTØNAD -> MålgruppeType.OMSTILLINGSSTØNAD
        }

    private fun MålgruppeType.tilTypeYtelsePeriode() =
        when (this) {
            MålgruppeType.AAP -> TypeYtelsePeriode.AAP
            MålgruppeType.DAGPENGER -> TODO("Har ikke mapping for dagpenger ennå")
            MålgruppeType.OMSTILLINGSSTØNAD -> TypeYtelsePeriode.OMSTILLINGSSTØNAD
            MålgruppeType.OVERGANGSSTØNAD -> TypeYtelsePeriode.ENSLIG_FORSØRGER
            MålgruppeType.NEDSATT_ARBEIDSEVNE -> null
            MålgruppeType.UFØRETRYGD -> null
            MålgruppeType.SYKEPENGER_100_PROSENT -> null
            MålgruppeType.INGEN_MÅLGRUPPE -> null
        }
}

private fun List<no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode>.tilFellesFormat() =
    this.map {
        Vedtaksperiode(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            målgruppe = it.målgruppe,
            aktivitet = it.aktivitet,
        )
    }

private data class Vilkårperioder(
    val aktiviteter: List<InngangsvilkårAktivitet>,
    val målgrupper: List<InngangsvilkårMålgruppe>,
)

private data class InngangsvilkårMålgruppe(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
) : Periode<LocalDate>,
    Mergeable<LocalDate, InngangsvilkårMålgruppe> {
    constructor(vilkårperiode: VilkårperiodeMålgruppe) :
        this(
            fom = vilkårperiode.fom,
            tom = vilkårperiode.tom,
            målgruppe = vilkårperiode.faktaOgVurdering.type.vilkårperiodeType,
        )

    override fun merge(other: InngangsvilkårMålgruppe): InngangsvilkårMålgruppe =
        this.copy(fom = minOf(fom, other.fom), tom = maxOf(tom, other.tom))
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
            val prosentDiff =
                diff("prosent", prosent, registerAktivitet.prosentDeltakelse?.toInt()).takeIf { prosent != null }
            val antallDagerDiff =
                diff("dager", antallDager, registerAktivitet.antallDagerPerUke).takeIf { antallDager != null }
            val diff = listOfNotNull(fomDiff, tomDiff, prosentDiff, antallDagerDiff)
            if (diff.isNotEmpty()) {
                logger.info("Diff vilkår=$id vs register ${diff.joinToString(" ")}")
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
