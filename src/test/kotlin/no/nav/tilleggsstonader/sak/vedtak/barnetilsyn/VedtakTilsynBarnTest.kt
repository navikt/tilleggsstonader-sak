package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtakTilsynBarnTest {
    @Nested
    inner class InnvilgetVedtak {
        @Test
        fun `skal feile dersom innvilget vedtak ikke inneholder beregningsresultat`() {
            assertThatThrownBy {
                innvilgetVedtak(beregningsresultat = null)
            }.hasMessageContaining("Mangler beregningsresultat")
        }

        @Test
        fun `skal feile dersom innvilget vedtak ikke inneholder vedtak`() {
            assertThatThrownBy {
                innvilgetVedtak(vedtak = null)
            }.hasMessageContaining("Mangler vedtak")
        }
    }

    @Nested
    inner class AvslåttVedtak {
        @Test
        fun `skal feile om avslått vedtak ikke har årsaker`() {
            assertThatThrownBy {
                VedtakTilsynBarn(
                    behandlingId = UUID.randomUUID(),
                    type = TypeVedtak.AVSLAG,
                    avslagBegrunnelse = "begrunnelse",
                )
            }
                .hasMessage("Må velge minst en årsak for avslag")
        }

        @Test
        fun `skal feile om avslått vedtak ikke har årsak for avslag`() {
            assertThatThrownBy {
                VedtakTilsynBarn(
                    behandlingId = UUID.randomUUID(),
                    type = TypeVedtak.AVSLAG,
                    årsakAvslag = ÅrsakAvslag.Wrapper(listOf(ÅrsakAvslag.INGEN_AKTIVITET)),
                )
            }
                .hasMessage("Avslag må begrunnes")
        }
    }
}
