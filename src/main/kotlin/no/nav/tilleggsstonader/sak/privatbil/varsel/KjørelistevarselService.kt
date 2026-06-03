package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KjørelistevarselService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val kjørelisteService: KjørelisteService,
    private val vedtakService: VedtakService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun sendUkentligVarselOmKjørelister() {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            logger.info("Starter scheduled jobb for sending av kjøreliste-varsler")

            val behandlingerMedRammevedtak = behandlingService.finnGjeldendeIverksatteBehandlingerMedRammevedtakPrivatBil()

            logger.info("Fant ${behandlingerMedRammevedtak.size} behandlinger med rammevedtak for privat bil")

            val varselTasker =
                behandlingerMedRammevedtak
                    .filter { skalSendeKjørelisteVarselForForrigeUke(it.id) }
                    .map { fagsakService.hentFagsak(it.fagsakId).fagsakPersonId }
                    .distinct()
                    .map { SendKjorelistevarselTilBrukerTask.opprett(it) }

            logger.info("Skal sende ${varselTasker.size} varsler om tilgjengelige kjørelister")

            if (varselTasker.isNotEmpty()) {
                taskService.saveAll(varselTasker)
            }

            logger.info("Fullført opprettelse av ${varselTasker.size} tasker for sending av varsel")
        } finally {
            MDC.clear()
        }
    }

    fun skalSendeKjørelisteVarselForForrigeUke(behandlingId: BehandlingId): Boolean {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling) || !erGjeldendeIverksatteBehandling(behandling)) return false

        val forrigeUke = UkeIÅr.forrigeUke()
        val forrigeUkeDatoPeriode =
            forrigeUke.alleDager().let {
                Datoperiode(it.first(), it.last())
            }

        val reiserMedPeriodeForrigeUke =
            finnRammevedtakPrivatBil(behandling)
                ?.reiser
                ?.filter { it.grunnlag.inneholder(forrigeUkeDatoPeriode) }
                ?: emptyList()

        val kjørelisterGruppertPåReiseId = kjørelisterGruppertPåReiseId(behandling.fagsakId)

        return reiserMedPeriodeForrigeUke
            .any { reise -> !kjørelisterGruppertPåReiseId.harLevertForUke(reise.reiseId, forrigeUke) }
    }

    fun skalSendeKjørelistevarselVedFerdigstillingAvBehandling(behandlingId: BehandlingId): Boolean {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        if (behandling.type == BehandlingType.KJØRELISTE) return false
        if (!erBehandlingInnvilgelseEllerOpphørDagligReise(behandling) || !erGjeldendeIverksatteBehandling(behandling)) return false

        val rammevedtakReiser = finnRammevedtakPrivatBil(behandling)?.reiser ?: emptyList()
        val kjørelister = kjørelisterGruppertPåReiseId(behandling.fagsakId)

        return rammevedtakReiser.any {
            finnesTidligereUkerUtenInnsendtKjøreliste(it, kjørelister[it.reiseId] ?: emptyList())
        }
    }

    private fun finnesTidligereUkerUtenInnsendtKjøreliste(
        rammeForReiseMedPrivatBil: RammeForReiseMedPrivatBil,
        kjørelisterForReise: List<Kjøreliste>,
    ): Boolean {
        val nåværendeUke = UkeIÅr.nå()
        val alleUkerIRammevedtak =
            rammeForReiseMedPrivatBil.grunnlag
                .alleDatoerGruppertPåUke()
                .keys
                .filter { it < nåværendeUke }
        val alleInnsendteUker =
            kjørelisterForReise
                .flatMap { it.data.alleDatoer() }
                .map { it.tilUkeIÅr() }
                .toSet()

        return (alleUkerIRammevedtak - alleInnsendteUker).isNotEmpty()
    }

    private fun erBehandlingInnvilgelseEllerOpphørDagligReise(behandling: Saksbehandling) =
        behandling.stønadstype.gjelderDagligReise() &&
            (behandling.resultat == BehandlingResultat.INNVILGET || behandling.resultat == BehandlingResultat.OPPHØRT)

    private fun erGjeldendeIverksatteBehandling(behandling: Saksbehandling) =
        behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)?.id == behandling.id

    private fun finnRammevedtakPrivatBil(behandling: Saksbehandling): RammevedtakPrivatBil? =
        vedtakService
            .hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandling.id)
            .data.rammevedtakPrivatBil

    private fun kjørelisterGruppertPåReiseId(fagsakId: FagsakId): Map<ReiseId, List<Kjøreliste>> =
        kjørelisteService.hentForFagsakId(fagsakId).groupBy { it.data.reiseId }
}

private fun Map<ReiseId, List<Kjøreliste>>.harLevertForUke(
    reiseId: ReiseId,
    uke: UkeIÅr,
): Boolean =
    this[reiseId]?.any {
        it.data
            .alleDatoer()
            .map { dato -> dato.tilUkeIÅr() }
            .contains(uke)
    } == true
