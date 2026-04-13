package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.tilTema
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
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
import no.nav.tilleggsstonader.sak.oppfølging.domain.PeriodeMedÅpenTom
import no.nav.tilleggsstonader.sak.oppfølging.domain.mergeSammenhengende
import no.nav.tilleggsstonader.sak.oppfølging.kontroller.OppfølgingAktivitetKontrollerUtil
import no.nav.tilleggsstonader.sak.oppfølging.kontroller.OppfølgingMålgruppeKontrollerUtil
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilMålgruppe
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
    private val vedtakService: VedtakService,
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
    fun opprettTaskerForOppfølging(tema: Tema) {
        val tidspunkt = LocalDateTime.now()
        oppfølgingRepository.markerAlleAktiveSomIkkeAktive(tema)
        Stønadstype.entries.forEach { stønadstype ->
            if (stønadstype.tilTema() == tema) {
                val behandlinger = behandlingRepository.finnGjeldendeIverksatteBehandlinger(stønadstype = stønadstype)
                taskService.saveAll(behandlinger.map { OppfølgingTask.opprettTask(it.id, tidspunkt) })
            }
        }
    }

    /**
     * Oppretter oppfølging for behandlingId.
     *
     * Oppretter ikke ny oppfølging hvis:
     * - sisteForFagsak ble ignorert og ny data er lik gammel data
     * - det finnes en aktiv oppfølging på behandlingen med utfall UNDER_ARBEID eller UTSETTES
     * - det finnes en aktiv oppfølging på behandlingen og data er uendret
     *
     * En aktiv oppfølging deaktiveres (erstattes) hvis den ikke er påstartet (kontrollert == null)
     * eller har utfall HÅNDTERT eller IGNORERES.
     *
     * UTSETTES er inkludert som aktiv fordi saksbehandler kan ha oppdaget avviket og utsatt i påvente
     * av svar fra bruker eller andre.
     */
    @Transactional
    fun opprettOppfølging(behandlingId: BehandlingId): Oppfølging? {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsakMetadata = fagsakService.hentMetadata(listOf(behandling.fagsakId)).values.single()

        val perioderForKontroll = hentPerioderForKontroll(behandling, fagsakMetadata)
        if (perioderForKontroll.isEmpty()) return null

        val data = OppfølgingData(perioderTilKontroll = perioderForKontroll)

        val sisteForFagsak = oppfølgingRepository.finnSisteForFagsak(behandlingId)

        if (sisteForFagsak?.kontrollert?.utfall == KontrollertUtfall.IGNORERES && sisteForFagsak.data == data) {
            logger.warn(
                "Ingen endring for behandling=$behandlingId siden oppfølging=${sisteForFagsak.id} " +
                    "ble kontrollert forrige gang, oppretter ikke ny oppfølging",
            )
            return null
        }

        val aktivOppfølging = oppfølgingRepository.finnAktivForBehandling(behandlingId)

        if (aktivOppfølging != null) {
            val aktivUtfall = aktivOppfølging.kontrollert?.utfall
            if (aktivUtfall == KontrollertUtfall.UNDER_ARBEID || aktivUtfall == KontrollertUtfall.UTSETTES) {
                logger.warn(
                    "Aktiv oppfølging=${aktivOppfølging.id} for behandling=$behandlingId er under arbeid " +
                        "(utfall=$aktivUtfall), oppretter ikke ny oppfølging",
                )
                return null
            }
            if (aktivOppfølging.data == data) {
                logger.warn(
                    "Aktiv oppfølging=${aktivOppfølging.id} for behandling=$behandlingId har uendret data, " +
                        "oppretter ikke ny oppfølging",
                )
                return null
            }
            oppfølgingRepository.markerAktivSomIkkeAktiv(behandlingId)
        }

        return oppfølgingRepository.insert(
            Oppfølging(behandlingId = behandlingId, data = data, tema = fagsakMetadata.stønadstype.tilTema()),
        )
    }

    private fun hentPerioderForKontroll(
        behandling: Behandling,
        fagsak: FagsakMetadata,
    ): List<PeriodeForKontroll> {
        val vedtaksperioder = vedtakService.hentVedtaksperioder(behandling.id).mergeSammenhengende()
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
    ): Map<MålgruppeType, List<PeriodeMedÅpenTom>> {
        if (målgrupper.isEmpty()) {
            return emptyMap()
        }
        val typer = målgrupper.flatMap { it.målgruppe.tilTypeYtelsePerioder() }.distinct()
        val fom = målgrupper.minOf { it.fom }
        val tom = målgrupper.maxOf { it.tom }
        return ytelseService
            .hentYtelser(fagsak.ident, fom = fom, tom = tom, typer)
            .also { validerResultat(it.kildeResultat) }
            .perioder
            .filter { it.aapErFerdigAvklart != true }
            .map { it.type.tilMålgruppe() to PeriodeMedÅpenTom(fom = it.fom, tom = it.tom) }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.mergeSammenhengende() }
    }

    private fun validerResultat(kildeResultat: List<YtelsePerioderDto.KildeResultatYtelse>) {
        val kildeResulatUtenOK = kildeResultat.filter { it.resultat != ResultatKilde.OK }

        feilHvis(kildeResulatUtenOK.isNotEmpty()) {
            "Feil ved henting av ytelser fra andre systemer: ${kildeResulatUtenOK.joinToString(
                ", ",
            ) { it.type.name }}. Prøv å laste inn siden på nytt."
        }
    }

    private fun MålgruppeType.tilTypeYtelsePerioder(): List<TypeYtelsePeriode> =
        when (this) {
            MålgruppeType.AAP -> listOf(TypeYtelsePeriode.AAP)
            MålgruppeType.DAGPENGER -> listOf(TypeYtelsePeriode.DAGPENGER)
            MålgruppeType.OMSTILLINGSSTØNAD -> listOf(TypeYtelsePeriode.OMSTILLINGSSTØNAD)
            MålgruppeType.OVERGANGSSTØNAD -> listOf(TypeYtelsePeriode.ENSLIG_FORSØRGER)
            MålgruppeType.TILTAKSPENGER -> listOf(TypeYtelsePeriode.TILTAKSPENGER_TPSAK, TypeYtelsePeriode.TILTAKSPENGER_ARENA)

            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.UFØRETRYGD,
            MålgruppeType.SYKEPENGER_100_PROSENT,
            MålgruppeType.INGEN_MÅLGRUPPE,
            MålgruppeType.KVALIFISERINGSSTØNAD,
            MålgruppeType.INNSATT_I_FENGSEL,
            -> error("Skal ikke sjekke målgruppe=$this")
        }
}
