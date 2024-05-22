package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class TilsynBarnVedtakServiceTest {

    private val tilsynBarnVedtakService = TilsynBarnVedtakService(mockk(), mockk(), mockk())

    @Test
    fun `skal mappe innvilget vedtak til dto`() {
        val vedtak = innvilgetVedtak()

        val dto = tilsynBarnVedtakService.mapTilDto(vedtak) as InnvilgelseTilsynBarnDto

        assertThat(dto.utgifter).isEqualTo(vedtak.vedtak?.utgifter)
        assertThat(dto.beregningsresultat!!.perioder).isEqualTo(vedtak.beregningsresultat!!.perioder)
    }

    @Test
    fun `skal mappe avslått vedtak til dto`() {
        val vedtak = VedtakTilsynBarn(
            behandlingId = UUID.randomUUID(),
            type = TypeVedtak.AVSLAG,
            avslagBegrunnelse = "begrunnelse",
            årsakAvslag = ÅrsakAvslag.Wrapper(årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET)),
        )

        val dto = tilsynBarnVedtakService.mapTilDto(vedtak) as AvslagTilsynBarnDto

        assertThat(dto.begrunnelse).isEqualTo(vedtak.avslagBegrunnelse)
        assertThat(dto.type).isEqualTo(vedtak.type)
    }
}
