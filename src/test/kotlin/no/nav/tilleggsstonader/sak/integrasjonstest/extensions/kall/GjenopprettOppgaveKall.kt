package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

// Må kalles med utvikler-rettighet
class GjenopprettOppgaveKall(
    private val testklient: Testklient,
) {
    fun gjenopprett(behandlingId: BehandlingId) =
        apiRespons
            .gjenopprett(behandlingId)
            .expectStatus()
            .isNoContent
            .expectBody()
            .isEmpty()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = GjenopprettOppgaveApi()

    inner class GjenopprettOppgaveApi {
        fun gjenopprett(behandlingId: BehandlingId) = testklient.post("/api/forvaltning/oppgave/gjenopprett/$behandlingId", Unit)
    }
}
