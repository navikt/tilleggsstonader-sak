package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.behandling.dto.BehandlingDto
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.BehandlingshistorikkDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class BehandlingKall(
    private val testklient: Testklient,
) {
    fun hent(behandlingId: BehandlingId) = apiRespons.hent(behandlingId).expectOkWithBody<BehandlingDto>()

    fun ekstern(eksternId: Long) = apiRespons.ekstern(eksternId).expectOkWithBody<BehandlingId>()

    fun henlegg(
        behandlingId: BehandlingId,
        dto: HenlagtDto,
    ) = apiRespons
        .henlegg(behandlingId, dto)
        .expectStatus()
        .isNoContent
        .expectBody()
        .isEmpty()

    fun historikk(behandlingId: BehandlingId): List<BehandlingshistorikkDto> = apiRespons.historikk(behandlingId).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = BehandlingApi()

    inner class BehandlingApi {
        fun hent(behandlingId: BehandlingId) = testklient.get("/api/behandling/$behandlingId")

        fun ekstern(eksternId: Long) = testklient.get("/api/behandling/ekstern/$eksternId")

        fun henlegg(
            behandlingId: BehandlingId,
            request: HenlagtDto,
        ) = testklient.post("/api/behandling/$behandlingId/henlegg", request)

        fun historikk(behandlingId: BehandlingId) = testklient.get("/api/behandlingshistorikk/$behandlingId")
    }
}
