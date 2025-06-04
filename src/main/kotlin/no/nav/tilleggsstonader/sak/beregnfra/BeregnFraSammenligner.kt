package no.nav.tilleggsstonader.sak.beregnfra

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class BeregnFraSammenligner(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val utledBeregnFraDatoService: UtledBeregnFraDatoService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val mdcBeregnFraProsessKjøringKey = "beregnFraProsessKjoering"

    @Transactional(readOnly = true)
    fun sammenlignRevurderFraMedBeregnFra() {
        try {
            MDC.put(mdcBeregnFraProsessKjøringKey, UUID.randomUUID().toString())
            logger.info("Starter sammenligning av revurderFra med utledet beregnFra for alle fagsaker og behandlinger")
            fagsakService
                .hentAlleFagsakIder()
                .forEach { sammenlignRevurderFraMedBeregnFraForFagsak(it) }
        } finally {
            MDC.remove(mdcBeregnFraProsessKjøringKey)
        }
    }

    private fun sammenlignRevurderFraMedBeregnFraForFagsak(fagsakId: FagsakId) {
        behandlingService
            .hentBehandlinger(fagsakId)
            .filter { it.forrigeIverksatteBehandlingId != null && it.revurderFra != null }
            .forEach { sammenlignRevurderFraMedBeregnFraForBehandling(it) }
    }

    private fun sammenlignRevurderFraMedBeregnFraForBehandling(behandling: Behandling) {
        try {
            val revurderFraDato = behandling.revurderFra
            val beregnFraDato = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

            if (revurderFraDato == beregnFraDato) {
                logger.info(
                    "For fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, beregnFra=$beregnFraDato - BeregnFraDato er lik RevurderFraDato",
                )
            } else if (beregnFraDato == null) {
                logger.info(
                    "For fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, beregnFra=$beregnFraDato - Kunne ikke utlede beregnFraDato",
                )
            } else if (beregnFraDato < revurderFraDato) {
                logger.warn(
                    "For fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, beregnFra=$beregnFraDato - BeregnFraDato er før RevurderFraDato",
                )
            } else {
                logger.warn(
                    "For fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, beregnFra=$beregnFraDato - BeregnFraDato er etter RevurderFraDato",
                )
            }
        } catch (e: Exception) {
            logger.info("Kunne ikke utlede beregnFraDato for behandling=${behandling.id}, fagsak=${behandling.fagsakId}", e)
        }
    }
}
