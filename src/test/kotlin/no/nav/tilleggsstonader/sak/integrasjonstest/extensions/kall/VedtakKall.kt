package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest

class VedtakKall(
    val test: IntegrationTest,
) {
    val tilsynBarn = VedtakStønadKall(test, "tilsyn-barn")
    val læremidler = VedtakStønadKall(test, "laremidler")
    val boutgifter = VedtakStønadKall(test, "boutgifter")
    val dagligReise = VedtakStønadKall(test, "daglig-reise")

    fun foreslåVedtaksperioder(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vedtak/$behandlingId/foresla")
                .medOnBehalfOfToken()
                .exchange()
        }
}

class VedtakStønadKall(
    val test: IntegrationTest,
    val stønadPath: String,
) {
    fun lagreVedtak(
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

    fun lagreOpphør(
        behandlingId: BehandlingId,
        opphørDto: VedtakRequest,
    ) = lagreVedtak(behandlingId, "opphor", opphørDto)

    fun lagreAvslag(
        behandlingId: BehandlingId,
        avslagDto: VedtakRequest,
    ) = lagreVedtak(behandlingId, "avslag", avslagDto)

    fun lagreInnvilgelse(
        behandlingId: BehandlingId,
        innvilgelseDto: VedtakRequest,
    ) = lagreVedtak(behandlingId, "innvilgelse", innvilgelseDto)

    fun hentVedtak(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vedtak/$stønadPath/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
        }
}
