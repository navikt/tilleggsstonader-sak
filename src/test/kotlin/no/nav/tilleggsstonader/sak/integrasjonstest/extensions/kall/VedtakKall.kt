package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest

class VedtakKall(
    private val testklient: Testklient,
) {
    fun hentVedtak(
        stønadstype: Stønadstype,
        behandlingId: BehandlingId,
    ) = apiRespons
        .hentVedtak(stønadstype, behandlingId)
        .expectStatus()
        .isOk

    fun foreslåVedtaksperioder(behandlingId: BehandlingId): List<LagretVedtaksperiodeDto> =
        apiRespons.foresåVedtaksperioder(behandlingId).expectOkWithBody()

    fun lagreVedtak(
        stønadstype: Stønadstype,
        behandlingId: BehandlingId,
        typeVedtakPath: String,
        vedtakDto: VedtakRequest,
    ) {
        apiRespons
            .lagreVedtak(stønadstype, behandlingId, typeVedtakPath, vedtakDto)
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

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VedtakApi()

    inner class VedtakApi {
        fun hentVedtak(
            stønadstype: Stønadstype,
            behandlingId: BehandlingId,
        ) = testklient.get("/api/vedtak/${stønadstype.tilPath()}/$behandlingId")

        fun foresåVedtaksperioder(behandlingId: BehandlingId) = testklient.get("/api/vedtak/$behandlingId/foresla")

        fun lagreVedtak(
            stønadstype: Stønadstype,
            behandlingId: BehandlingId,
            typeVedtakPath: String,
            vedtakDto: VedtakRequest,
        ) = testklient.post("/api/vedtak/${stønadstype.tilPath()}/$behandlingId/$typeVedtakPath", vedtakDto)

        fun lagreInnvilgelse(
            stønadstype: Stønadstype,
            behandlingId: BehandlingId,
            innvilgelseDto: VedtakRequest,
        ) = lagreVedtak(stønadstype, behandlingId, "innvilgelse", innvilgelseDto)
    }
}

private fun Stønadstype.tilPath(): String =
    when (this) {
        Stønadstype.BARNETILSYN -> "tilsyn-barn"
        Stønadstype.LÆREMIDLER -> "laremidler"
        Stønadstype.BOUTGIFTER -> "boutgifter"
        Stønadstype.DAGLIG_REISE_TSO -> "daglig-reise"
        Stønadstype.DAGLIG_REISE_TSR -> "daglig-reise"
    }
