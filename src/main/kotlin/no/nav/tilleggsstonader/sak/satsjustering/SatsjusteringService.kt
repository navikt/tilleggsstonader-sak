package no.nav.tilleggsstonader.sak.satsjustering

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SatsjusteringService(
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun kjørSatsjustering(behandlingId: BehandlingId) {
        val behandlingSomTrengerSatsjustering = behandlingService.hentBehandling(behandlingId)
        val fagsakId = behandlingSomTrengerSatsjustering.fagsakId
        if (behandlingService.finnesIkkeFerdigstiltBehandling(fagsakId)) {
            logger.info("Finnes en ikke ferdigstilt behandling for fagsakId=$fagsakId, kan ikke kjøre satsjustering.")
            return
        }
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
        if (sisteIverksatteBehandling?.id != behandlingId) {
            logger.info(
                "Siste iverksatte behandling=${sisteIverksatteBehandling?.id} er ikke lik behandlingId=$behandlingId, kan ikke kjøre satsjustering.",
            )
            return
        }

        // behandlingService.finnSisteBehandlingSomHarVedtakPåFagsaken()
        // sjekk om det finnes en nyere behandling på fagsaken
    }
}
