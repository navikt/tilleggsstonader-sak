package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange

class LæremidlerVedtakControllerTest : IntegrationTest() {
    val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
    val behandling = behandling(fagsak = fagsak, steg = StegType.BEREGNE_YTELSE, status = BehandlingStatus.UTREDES)

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
    }

    @Test
    fun `skal lagre og hente avslag`() {
        val vedtak =
            AvslagLæremidlerDto(
                årsakerAvslag = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                begrunnelse = "begrunnelse",
            )

        avslåVedtak(behandling, vedtak)

        val lagretDto = hentVedtak<AvslagLæremidlerDto>(behandling.id).body!!

        assertThat(lagretDto.årsakerAvslag).isEqualTo(vedtak.årsakerAvslag)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.AVSLAG)
    }

    private fun avslåVedtak(
        behandling: Behandling,
        vedtak: AvslagLæremidlerDto,
    ) {
        restTemplate.exchange<Map<String, Any>?>(
            localhost("api/vedtak/laremidler/${behandling.id}/avslag"),
            HttpMethod.POST,
            HttpEntity(vedtak, headers),
        )
    }

    private inline fun <reified T : VedtakLæremidlerResponse> hentVedtak(behandlingId: BehandlingId) =
        restTemplate.exchange<T>(
            localhost("api/vedtak/laremidler/$behandlingId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
}
