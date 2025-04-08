package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingAktivitet
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingAktivitet.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingMålgruppe
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingMålgruppe.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingRegisterAktiviteter
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksdata
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

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
class OppfølgingOpprettKontrollerService(
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
        val vedtaksperioder = hentVedtaksperioder(fagsak, behandling).mergeSammenhengende()
        if (vedtaksperioder.isEmpty()) {
            return emptyList()
        }

        val periode = Datoperiode(fom = vedtaksperioder.minOf { it.fom }, tom = vedtaksperioder.maxOf { it.tom })
        val inngangsvilkår = hentInngangsvilkår(behandling)

        val kontrollerAktivitet = finnKontrollerAktiviteter(fagsak, periode, inngangsvilkår, vedtaksperioder)

        val kontrollerMålgrupper = finnKontrollerMålgrupper(fagsak, inngangsvilkår, vedtaksperioder)
        return (kontrollerAktivitet + kontrollerMålgrupper).filter { it.trengerKontroll() }
    }

    private fun finnKontrollerAktiviteter(
        fagsak: FagsakMetadata,
        periode: Datoperiode,
        inngangsvilkår: List<Vilkårperiode>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<PeriodeForKontroll> {
        val registerAktiviteter = hentAktiviteter(fagsak, periode)
        val aktiviteter = fraVilkårperioder(inngangsvilkår, registerAktiviteter)

        return vedtaksperioder.map { it.finnEndringer(registerAktiviteter, aktiviteter) }
    }

    private fun finnKontrollerMålgrupper(
        fagsak: FagsakMetadata,
        inngangsvilkår: List<Vilkårperiode>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<PeriodeForKontroll> {
        val målgrupper = fraVilkårperioder(inngangsvilkår)
        val registerYtelser = hentYtelser(fagsak, målgrupper)

        return målgrupper
            .map {
                val endringer =
                    it
                        .finnEndringer(vedtaksperioder, registerYtelser)
                        .utenAAPSomGjelderFraOgMedNesteMåned(målgruppe = it.målgruppe)

                PeriodeForKontroll(
                    fom = it.fom,
                    tom = it.tom,
                    type = it.målgruppe,
                    endringer = endringer,
                )
            }
    }

    private fun hentVedtaksperioder(
        fagsak: FagsakMetadata,
        behandling: Behandling,
    ): List<Vedtaksperiode> =
        when (fagsak.stønadstype) {
            Stønadstype.BARNETILSYN ->
                hentVedtak<InnvilgelseEllerOpphørTilsynBarn>(behandling).vedtaksperioder ?: emptyList()

            Stønadstype.LÆREMIDLER ->
                hentVedtak<InnvilgelseEllerOpphørLæremidler>(behandling).vedtaksperioder.tilFellesFormat()

            Stønadstype.BOUTGIFTER ->
                hentVedtak<InnvilgelseEllerOpphørBoutgifter>(behandling).vedtaksperioder ?: emptyList()
        }

    private inline fun <reified T : Vedtaksdata> hentVedtak(behandling: Behandling) =
        vedtakRepository.findByIdOrThrow(behandling.id).withTypeOrThrow<T>().data

    private fun Vedtaksperiode.finnEndringer(
        oppfølgingRegisterAktiviteter: OppfølgingRegisterAktiviteter,
        inngangsvilkår: List<OppfølgingAktivitet>,
    ): PeriodeForKontroll {
        val endringerAktivitet = finnEndringIAktivitet(oppfølgingRegisterAktiviteter, inngangsvilkår)
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
    private fun OppfølgingMålgruppe.finnEndringer(
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
                finnEndringFomTom(vedtaksperiode, snitt)
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

    private fun Vedtaksperiode.finnEndringIAktivitet(
        oppfølgingRegisterAktiviteter: OppfølgingRegisterAktiviteter,
        aktiviteter: List<OppfølgingAktivitet>,
    ): List<Kontroll> {
        val kontroller =
            when (this.aktivitet) {
                AktivitetType.REELL_ARBEIDSSØKER -> mutableListOf() // Skal ikke kontrolleres
                AktivitetType.INGEN_AKTIVITET -> error("Skal ikke være mulig å ha en stønadsperiode med ingen aktivitet")
                AktivitetType.TILTAK ->
                    finnEndringIRegisteraktivitetEllerAlle(this, aktiviteter, oppfølgingRegisterAktiviteter.tiltak)

                AktivitetType.UTDANNING ->
                    finnEndringIRegisteraktivitetEllerAlle(
                        this,
                        aktiviteter,
                        oppfølgingRegisterAktiviteter.utdanningstiltak,
                    )
            }

        val ingenTreff = kontroller.any { it.årsak == ÅrsakKontroll.INGEN_TREFF }
        if (ingenTreff && oppfølgingRegisterAktiviteter.alleAktiviteter.any { it.inneholder(this) }) {
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
        aktiviteter: List<OppfølgingAktivitet>,
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
     * Kontrollerer om endring i registeraktivitet påvirker snittet av en [Vedtaksperiode] og [OppfølgingAktivitet]
     * En registeraktivitet kan ha endret seg, men det er ikke sikkert endringen påvirker vedtaksperioden
     * Hvis man har flere aktiviteter som løper parallellt og en av de
     *
     * @param registerperiode er trukket ut fra [aktivitet] men er not null
     */
    private fun kontrollerEndringerMotRegisterAktivitet(
        vedtaksperiode: Vedtaksperiode,
        aktivitet: OppfølgingAktivitet,
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

    private fun hentInngangsvilkår(behandling: Behandling): List<Vilkårperiode> =
        vilkårperiodeRepository.findByBehandlingIdAndResultat(behandling.id, ResultatVilkårperiode.OPPFYLT)

    private fun hentAktiviteter(
        fagsak: FagsakMetadata,
        periode: Datoperiode,
    ) = registerAktivitetService
        .hentAktiviteterForGrunnlagsdata(
            ident = fagsak.ident,
            fom = periode.fom,
            tom = periode.tom,
        ).let { OppfølgingRegisterAktiviteter(it) }

    private fun hentYtelser(
        fagsak: FagsakMetadata,
        målgrupper: List<OppfølgingMålgruppe>,
    ): Map<MålgruppeType, List<Datoperiode>> {
        val typerSomSkalHentes =
            målgrupper
                .map { periode ->
                    periode.målgruppe.tilTypeYtelsePeriode().let { it to periode }
                }
        if (typerSomSkalHentes.isEmpty()) {
            return emptyMap()
        }
        val typer = typerSomSkalHentes.map { it.first }.distinct()
        val fom = typerSomSkalHentes.minOf { it.second.fom }
        val tom = typerSomSkalHentes.maxOf { it.second.tom }
        return ytelseService
            .hentYtelser(fagsak.ident, fom = fom, tom = tom, typer)
            .perioder
            .filter { it.aapErFerdigAvklart != true }
            .filter { it.tom != null }
            .map { it.type.tilMålgruppe() to Datoperiode(fom = it.fom, tom = it.tom!!) }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.mergeSammenhengende() }
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

            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE,
            -> error("Skal ikke sjekke målgruppe=$this")
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
