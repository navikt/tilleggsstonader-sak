package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingVedtakRequest
import org.springframework.test.web.servlet.client.RestTestClient

class BeregnReiseTilSamlingKall(
    private val testklient: Testklient,
) {
    fun beregn(
        behandlingId: BehandlingId,
        vedtakDto: InnvilgelseReiseTilSamlingVedtakRequest,
    ): RestTestClient.ResponseSpec = apiRespons.beregn(behandlingId, vedtakDto)

    val apiRespons = BeregnReiseTilSamlingApi()

    inner class BeregnReiseTilSamlingApi {
        fun beregn(
            behandlingId: BehandlingId,
            vedtakDto: InnvilgelseReiseTilSamlingVedtakRequest,
        ) = testklient.post(
            "/api/vedtak/reise-til-samling/$behandlingId/beregn",
            vedtakDto,
        )
    }
}
