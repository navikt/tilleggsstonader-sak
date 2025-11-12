package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest

class VedtakKall(
    private val test: IntegrationTest,
) {
    fun hentVedtak(
        stønadstype: Stønadstype,
        behandlingId: BehandlingId,
    ) = with(test) {
        webTestClient
            .get()
            .uri("/api/vedtak/${stønadstype.tilPath()}/$behandlingId")
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isOk
    }

    fun foreslåVedtaksperioder(behandlingId: BehandlingId): List<LagretVedtaksperiodeDto> =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vedtak/$behandlingId/foresla")
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<List<LagretVedtaksperiodeDto>>()
        }

    fun lagreVedtak(
        stønadstype: Stønadstype,
        behandlingId: BehandlingId,
        typeVedtakPath: String,
        vedtakDto: VedtakRequest,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/vedtak/${stønadstype.tilPath()}/$behandlingId/$typeVedtakPath")
            .bodyValue(vedtakDto)
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isOk
    }

    fun lagreOpphør(
        stønadstype: Stønadstype,
        behandlingId: BehandlingId,
        opphørDto: VedtakRequest,
    ) = lagreVedtak(stønadstype, behandlingId, "opphor", opphørDto)

    fun lagreAvslag(
        stønadstype: Stønadstype,
        behandlingId: BehandlingId,
        avslagDto: VedtakRequest,
    ) = lagreVedtak(stønadstype, behandlingId, "avslag", avslagDto)

    fun lagreInnvilgelse(
        stønadstype: Stønadstype,
        behandlingId: BehandlingId,
        innvilgelseDto: VedtakRequest,
    ) = lagreVedtak(stønadstype, behandlingId, "innvilgelse", innvilgelseDto)
}

private fun Stønadstype.tilPath(): String =
    when (this) {
        Stønadstype.BARNETILSYN -> "tilsyn-barn"
        Stønadstype.LÆREMIDLER -> "laremidler"
        Stønadstype.BOUTGIFTER -> "boutgifter"
        Stønadstype.DAGLIG_REISE_TSO -> "daglig-reise"
        Stønadstype.DAGLIG_REISE_TSR -> "daglig-reise"
    }
