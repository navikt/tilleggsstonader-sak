package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class TilsynBarnVedtakServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val tilsynBarnVedtakService = TilsynBarnVedtakService(mockk(), mockk(), mockk(), behandlingService)

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentSaksbehandling(any<BehandlingId>()) } returns saksbehandling()
    }

    @Test
    fun `skal mappe innvilget vedtak til dto`() {
        val vedtak = innvilgetVedtak()

        val dto = tilsynBarnVedtakService.mapTilDto(vedtak)

        assertThat(dto).isInstanceOf(InnvilgelseTilsynBarnDto::class.java)
    }

    @Test
    fun `skal mappe avslått vedtak til dto`() {
        val vedtak = VedtakTilsynBarn(
            behandlingId = BehandlingId.random(),
            type = TypeVedtak.AVSLAG,
            avslagBegrunnelse = "begrunnelse",
            årsakerAvslag = ÅrsakAvslag.Wrapper(årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET)),
        )

        val dto = tilsynBarnVedtakService.mapTilDto(vedtak) as AvslagTilsynBarnDto

        assertThat(dto.begrunnelse).isEqualTo(vedtak.avslagBegrunnelse)
        assertThat(dto.type).isEqualTo(vedtak.type)
    }
}
