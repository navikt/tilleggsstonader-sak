package no.nav.tilleggsstonader.sak.tidligsteendring

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
class TidligsteEndringRevurderFraSammenligner(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val mdcTidligsteEndringProsessKjøringKey = "tidligsteEndringProsessKjoering"

    @Transactional(readOnly = true)
    fun sammenlignRevurderFraMedTidligsteEndring() {
        try {
            MDC.put(mdcTidligsteEndringProsessKjøringKey, UUID.randomUUID().toString())
            logger.info("Starter sammenligning av revurderFra med utledet tidligste endring for alle fagsaker og behandlinger")
            fagsakService
                .hentAlleFagsakIder()
                .forEach { sammenlignRevurderFraMedTidligsteEndringForFagsak(it) }
        } finally {
            MDC.remove(mdcTidligsteEndringProsessKjøringKey)
        }
    }

    private fun sammenlignRevurderFraMedTidligsteEndringForFagsak(fagsakId: FagsakId) {
        behandlingService
            .hentBehandlinger(fagsakId)
            .filter { it.forrigeIverksatteBehandlingId != null && it.revurderFra != null && it.erAvsluttet() }
            .forEach { sammenlignRevurderFraMedTidligsteEndringForBehandling(it) }
    }

    private fun sammenlignRevurderFraMedTidligsteEndringForBehandling(behandling: Behandling) {
        try {
            val revurderFraDato = behandling.revurderFra
            val tidligsteEndring = utledTidligsteEndringService.utledTidligsteEndring(behandling.id)

            if (revurderFraDato == tidligsteEndring) {
                logger.info(
                    "Tidligste endring er lik RevurderFraDato - fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, tidligste endring=$tidligsteEndring",
                )
            } else if (tidligsteEndring == null) {
                logger.info(
                    "Kunne ikke utlede tidligste endring - fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, tidligste endring=$tidligsteEndring",
                )
            } else if (tidligsteEndring < revurderFraDato) {
                logger.info(
                    "Tidligste endring er før RevurderFraDato - fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, tidligste endring=$tidligsteEndring",
                )
            } else {
                logger.info(
                    "Tidligste endring er etter RevurderFraDato - for fagsak=${behandling.fagsakId}, behandling=${behandling.id}, revurderFra=$revurderFraDato, tidligste endring=$tidligsteEndring",
                )
            }
        } catch (e: Exception) {
            logger.info("Kunne ikke utlede tidligste endring for behandling=${behandling.id}, fagsak=${behandling.fagsakId}", e)
        }
    }
}
