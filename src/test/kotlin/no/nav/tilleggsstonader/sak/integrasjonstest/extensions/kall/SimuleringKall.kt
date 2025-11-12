package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.dto.SimuleringDto

class SimuleringKall(
    private val testklient: Testklient,
) {
    fun simulering(behandlingId: BehandlingId): SimuleringDto = apiRespons.simulering(behandlingId).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = SimuleringApi()

    inner class SimuleringApi {
        fun simulering(behandlingId: BehandlingId) = testklient.get("/api/simulering/$behandlingId")
    }
}
