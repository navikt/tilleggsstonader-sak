package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

class VedtakKall(
    test: IntegrationTest,
) {
    val tilsynBarn = VedtakStønadKall(test, "tilsyn-barn")
    val læremidler = VedtakStønadKall(test, "laremidler")
    val boutgifter = VedtakStønadKall(test, "boutgifter")
    val dagligReise = VedtakStønadKall(test, "daglig-reise")
}

class VedtakStønadKall(
    val test: IntegrationTest,
    val stønadPath: String,
) {
    fun lagreVedtakResponse(
        behandlingId: BehandlingId,
        resultatPath: String,
        vedtakDto: VedtakRequest,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/vedtak/$stønadPath/$behandlingId/$resultatPath")
            .bodyValue(vedtakDto)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun lagreOpphørResponse(
        behandlingId: BehandlingId,
        opphørDto: VedtakRequest,
    ) = lagreVedtakResponse(behandlingId, "opphor", opphørDto)

    fun lagreAvslagResponse(
        behandlingId: BehandlingId,
        avslagDto: VedtakRequest,
    ) = lagreVedtakResponse(behandlingId, "avslag", avslagDto)

    fun lagreInnvilgelseResponse(
        behandlingId: BehandlingId,
        innvilgelseDto: VedtakRequest,
    ) = lagreVedtakResponse(behandlingId, "innvilgelse", innvilgelseDto)

    fun hentVedtakResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vedtak/$stønadPath/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
        }

    inline fun <reified T : VedtakResponse> hentVedtak(behandlingId: BehandlingId) =
        hentVedtakResponse(behandlingId)
            .expectOkWithBody<T>()
}
