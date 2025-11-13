package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class StegKall(
    private val testklient: Testklient,
) {
    fun reset(
        behandlingId: BehandlingId,
        resetStegRequest: StegController.ResetStegRequest,
    ) {
        apiRespons
            .reset(behandlingId, resetStegRequest)
            .expectOkEmpty()
    }

    fun ferdigstill(
        behandlingId: BehandlingId,
        dto: StegController.FerdigstillStegRequest,
    ): BehandlingId = apiRespons.ferdigstill(behandlingId, dto).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = StegApi()

    inner class StegApi {
        fun reset(
            behandlingId: BehandlingId,
            resetStegRequest: StegController.ResetStegRequest,
        ) = testklient.post("/api/steg/behandling/$behandlingId/reset", resetStegRequest)

        fun ferdigstill(
            behandlingId: BehandlingId,
            dto: StegController.FerdigstillStegRequest,
        ) = testklient.post("/api/steg/behandling/$behandlingId/ferdigstill", dto)
    }
}
