package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtakBeregningsresultat
import org.assertj.core.api.Assertions.assertThat
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

        /**
         * I tilfelle man setter opp Avslag/sealed class feil så er de ikke like
         */
        @Test
        fun `2 like avslag skal være like`() {
            val avslagTilsynBarn = AvslagTilsynBarn(listOf(ÅrsakAvslag.INGEN_AKTIVITET), "asd")
            val avslagTilsynBarn2 = AvslagTilsynBarn(listOf(ÅrsakAvslag.INGEN_AKTIVITET), "asd")

            assertThat(avslagTilsynBarn).isEqualTo(avslagTilsynBarn2)
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

        /**
         * I tilfelle man setter opp Avslag/sealed class feil så er de ikke like
         */
        @Test
        fun `2 like opphør skal være like`() {
            val avslagTilsynBarn =
                OpphørTilsynBarn(vedtakBeregningsresultat, listOf(ÅrsakOpphør.ENDRING_UTGIFTER), "asd")
            val avslagTilsynBarn2 =
                OpphørTilsynBarn(vedtakBeregningsresultat, listOf(ÅrsakOpphør.ENDRING_UTGIFTER), "asd")

            assertThat(avslagTilsynBarn).isEqualTo(avslagTilsynBarn2)
        }
    }
}
