package no.nav.tilleggsstonader.sak.utbetaling.fagomrade

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.kontrakter.felles.gjelderReiseTilSamling
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import org.springframework.stereotype.Service

@Service
class FagsakUtbetalingsvalgService(
    private val fagsakService: FagsakService,
    private val unleashService: UnleashService,
) {
    fun hentEllerSettUtbetalPåNyttFagområde(
        fagsakId: FagsakId,
        stønadstype: Stønadstype,
    ): Boolean {
        val utbetalPåNyttFagområde = fagsakService.hentFagsak(fagsakId).utbetalPåNyttFagområde
        if (utbetalPåNyttFagområde != null) {
            return utbetalPåNyttFagområde
        }
        return fagsakService.settUtbetalPåNyttFagområde(fagsakId, utledUtbetalPåNyttFagområde(stønadstype))
    }

    private fun utledUtbetalPåNyttFagområde(stønadstype: Stønadstype): Boolean =
        if (stønadstype.skalBrukeNyttFagområde()) {
            true
        } else {
            unleashService.isEnabled(Toggle.BRUK_NYTT_FAGOMRADE_FOR_UTBETALING)
        }

    private fun Stønadstype.skalBrukeNyttFagområde(): Boolean = gjelderDagligReise() || gjelderReiseTilSamling()
}
