package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtakBeregningsresultat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VedtakTilsynBarnTest {

    @Nested
    inner class AvslåttVedtak {
        @Test
        fun `skal feile om avslått vedtak ikke har årsaker`() {
            assertThatThrownBy {
                AvslagTilsynBarn(
                    årsaker = emptyList(),
                    begrunnelse = "begrunnelse",
                )
            }
                .hasMessage("Må velge minst en årsak for avslag")
        }

        @Test
        fun `skal feile om avslått vedtak ikke har årsak for avslag`() {
            assertThatThrownBy {
                AvslagTilsynBarn(
                    årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                    begrunnelse = "",
                )
            }
                .hasMessage("Avslag må begrunnes")
        }
    }

    @Nested
    inner class OpphørtVedtak {
        @Test
        fun `skal feile om opphørt vedtak ikke har årsaker`() {
            assertThatThrownBy {
                OpphørTilsynBarn(
                    beregningsresultat = vedtakBeregningsresultat,
                    årsaker = emptyList(),
                    begrunnelse = "begrunnelse",
                )
            }
                .hasMessage("Må velge minst en årsak for opphør")
        }

        @Test
        fun `skal feile om opphørt vedtak ikke har årsak for opphør`() {
            assertThatThrownBy {
                OpphørTilsynBarn(
                    beregningsresultat = vedtakBeregningsresultat,
                    årsaker = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                    begrunnelse = "",
                )
            }
                .hasMessage("Opphør må begrunnes")
        }
    }
}
