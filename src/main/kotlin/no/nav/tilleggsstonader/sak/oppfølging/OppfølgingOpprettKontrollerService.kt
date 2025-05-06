package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.ytelse.HentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakMetadata
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingInngangsvilkårAktivitet.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingInngangsvilkårMålgruppe
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingInngangsvilkårMålgruppe.Companion.fraVilkårperioder
import no.nav.tilleggsstonader.sak.oppfølging.domain.OppfølgingRegisterAktiviteter
import no.nav.tilleggsstonader.sak.oppfølging.kontroller.OppfølgingAktivitetKontrollerUtil
import no.nav.tilleggsstonader.sak.oppfølging.kontroller.OppfølgingMålgruppeKontrollerUtil
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

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

    /**
     * Oppretter oppfølging for behandlingId
     *
     * Hvis siste kontroll har utfall ignoreres og ny oppfølging er lik som sist opprettes ikke ny oppfølging
     */
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

        val inngangsvilkår = hentInngangsvilkår(behandling)
        val kontrollerAktivitet = finnKontrollerAktiviteter(fagsak, inngangsvilkår, vedtaksperioder)
        val kontrollerMålgrupper = finnKontrollerMålgrupper(fagsak, inngangsvilkår, vedtaksperioder)
        return (kontrollerAktivitet + kontrollerMålgrupper).filter { it.trengerKontroll() }
    }

    private fun finnKontrollerAktiviteter(
        fagsak: FagsakMetadata,
        inngangsvilkår: List<Vilkårperiode>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<PeriodeForKontroll> {
        val fom = vedtaksperioder.minOf { it.fom }
        val tom = vedtaksperioder.maxOf { it.tom }
        val registerAktiviteter = hentRegisterAktiviteter(fagsak, fom = fom, tom = tom)
        val aktiviteter = fraVilkårperioder(inngangsvilkår, registerAktiviteter)
        return OppfølgingAktivitetKontrollerUtil.finnEndringer(vedtaksperioder, registerAktiviteter, aktiviteter)
    }

    private fun finnKontrollerMålgrupper(
        fagsak: FagsakMetadata,
        inngangsvilkår: List<Vilkårperiode>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<PeriodeForKontroll> {
        val målgrupper = fraVilkårperioder(inngangsvilkår)
        val registerYtelser = hentRegisterYtelser(fagsak, målgrupper)
        return OppfølgingMålgruppeKontrollerUtil.finnEndringer(målgrupper, vedtaksperioder, registerYtelser)
    }

    private fun hentVedtaksperioder(
        fagsak: FagsakMetadata,
        behandling: Behandling,
    ): List<Vedtaksperiode> =
        when (fagsak.stønadstype) {
            Stønadstype.BARNETILSYN ->
                hentVedtak<InnvilgelseEllerOpphørTilsynBarn>(behandling).vedtaksperioder

            Stønadstype.LÆREMIDLER ->
                hentVedtak<InnvilgelseEllerOpphørLæremidler>(behandling).vedtaksperioder.tilFellesFormat()

            Stønadstype.BOUTGIFTER ->
                hentVedtak<InnvilgelseEllerOpphørBoutgifter>(behandling).vedtaksperioder
        }

    private inline fun <reified T : Vedtaksdata> hentVedtak(behandling: Behandling) =
        vedtakRepository.findByIdOrThrow(behandling.id).withTypeOrThrow<T>().data

    private fun hentInngangsvilkår(behandling: Behandling): List<Vilkårperiode> =
        vilkårperiodeRepository.findByBehandlingIdAndResultat(behandling.id, ResultatVilkårperiode.OPPFYLT)

    /**
     * Henter aktiviteter fra og til min/max av vedtaksperioder
     */
    private fun hentRegisterAktiviteter(
        fagsak: FagsakMetadata,
        fom: LocalDate,
        tom: LocalDate,
    ) = registerAktivitetService
        .hentAktiviteterForGrunnlagsdata(ident = fagsak.ident, fom = fom, tom = tom)
        .let { OppfølgingRegisterAktiviteter(it) }

    /**
     * Henter registerytelser fra/til min/max av tidligere målgrupper som finnes.
     * Då det kun er interessant å sjekke målgrupper i forhold til hva som finnes fra før
     */
    private fun hentRegisterYtelser(
        fagsak: FagsakMetadata,
        målgrupper: List<OppfølgingInngangsvilkårMålgruppe>,
    ): Map<MålgruppeType, List<Datoperiode>> {
        val typerSomSkalHentes =
            målgrupper.map { periode -> periode.målgruppe.tilTypeYtelsePeriode().let { it to periode } }

        if (typerSomSkalHentes.isEmpty()) {
            return emptyMap()
        }
        val typer = typerSomSkalHentes.map { it.first }.distinct()
        val fom = typerSomSkalHentes.minOf { it.second.fom }
        val tom = typerSomSkalHentes.maxOf { it.second.tom }
        return ytelseService
            .hentYtelser(fagsak.ident, fom = fom, tom = tom, typer)
            .also { validerResultat(it.hentetInformasjon) }
            .perioder
            .filter { it.aapErFerdigAvklart != true }
            .filter { it.tom != null }
            .map { it.type.tilMålgruppe() to Datoperiode(fom = it.fom, tom = it.tom!!) }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.mergeSammenhengende() }
    }

    private fun validerResultat(hentetInformasjon: List<HentetInformasjon>) {
        val test = hentetInformasjon.filter { it.status != StatusHentetInformasjon.OK }

        feilHvis(test.isNotEmpty()) {
            "Feil ved henting av ytelser fra andre systemer: ${test.joinToString(", ") { it.type.name }}. Prøv å laste inn siden på nytt."
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

            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE,
            -> error("Skal ikke sjekke målgruppe=$this")
        }

    /**
     * Mapper vedtaksperiode til felles format for å enklere kunne håndtere alle likt
     */
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
}
