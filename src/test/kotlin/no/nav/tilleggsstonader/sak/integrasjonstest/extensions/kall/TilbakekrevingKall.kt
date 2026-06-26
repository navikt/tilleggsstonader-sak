package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.tilbakekreving.dto.TilbakekrevingHendelseDto

class TilbakekrevingKall(
    private val testklient: Testklient,
) {
    fun hentHendelser(behandlingId: BehandlingId): List<TilbakekrevingHendelseDto> =
        apiRespons.hentHendelser(behandlingId).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktet slik at tester kan skrive egne assertions på responsen.
    val apiRespons = TilbakekrevingApi()

    inner class TilbakekrevingApi {
        fun hentHendelser(behandlingId: BehandlingId) = testklient.get("/api/tilbakekreving/$behandlingId/hendelser")
    }
}
