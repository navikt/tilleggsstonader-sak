package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import org.springframework.test.web.servlet.client.RestTestClient

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

        fun lagreEnhetsspesifiktVedtak(
            stønadstype: Stønadstype,
            behandlingId: BehandlingId,
            typeVedtakPath: String,
            vedtakDto: VedtakRequest,
            enhet: Enhet,
        ): RestTestClient.ResponseSpec =
            testklient.post(
                "/api/vedtak/${stønadstype.tilPath()}/$behandlingId/${enhet.tilPath()}/$typeVedtakPath",
                vedtakDto,
            )

        fun lagreInnvilgelse(
            stønadstype: Stønadstype,
            behandlingId: BehandlingId,
            innvilgelseDto: VedtakRequest,
        ): RestTestClient.ResponseSpec {
            if (stønadstype.gjelderDagligReise()) {
                return lagreEnhetsspesifiktVedtak(
                    stønadstype,
                    behandlingId,
                    "innvilgelse",
                    innvilgelseDto,
                    stønadstype.behandlendeEnhet(),
                )
            }
            return lagreVedtak(stønadstype, behandlingId, "innvilgelse", innvilgelseDto)
        }

        fun lagreOpphør(
            stønadstype: Stønadstype,
            behandlingId: BehandlingId,
            opphørDto: VedtakRequest,
        ) = lagreVedtak(stønadstype, behandlingId, "opphor", opphørDto)
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

private fun Enhet.tilPath(): String =
    when (this) {
        Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD -> "tso"
        Enhet.NAV_TILTAK_OSLO -> "tsr"
        Enhet.NAV_ARBEID_OG_YTELSER_EGNE_ANSATTE -> TODO()
        Enhet.VIKAFOSSEN -> TODO()
    }
