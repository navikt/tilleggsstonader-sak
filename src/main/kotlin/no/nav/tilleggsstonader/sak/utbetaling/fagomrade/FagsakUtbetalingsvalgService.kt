package no.nav.tilleggsstonader.sak.utbetaling.fagomrade

import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.springframework.stereotype.Service

@Service
class FagsakUtbetalingsvalgService(
    private val fagsakService: FagsakService,
) {
    fun hentEllerSettUtbetalPåNyttFagområde(fagsakId: FagsakId): Boolean {
        val utbetalPåNyttFagområde = fagsakService.hentFagsak(fagsakId).utbetalPåNyttFagområde
        if (utbetalPåNyttFagområde != null) {
            return utbetalPåNyttFagområde
        }
        return fagsakService.settUtbetalPåNyttFagområde(fagsakId, true)
    }
}
