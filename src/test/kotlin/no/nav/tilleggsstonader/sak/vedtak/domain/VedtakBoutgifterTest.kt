package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vedtaksperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class VedtakBoutgifterTest {
    @Nested
    inner class AvslåttVedtak {
        @Test
        fun `skal feile om avslått vedtak ikke har årsaker`() {
            assertThatThrownBy {
                AvslagBoutgifter(
                    årsaker = emptyList(),
                    begrunnelse = "begrunnelse",
                )
            }.hasMessage("Må velge minst en årsak for avslag")
        }

        @Test
        fun `skal feile om avslått vedtak ikke har årsak for avslag`() {
            assertThatThrownBy {
                AvslagBoutgifter(
                    årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                    begrunnelse = "",
                )
            }.hasMessage("Avslag må begrunnes")
        }

        /**
         * I tilfelle man setter opp Avslag/sealed class feil så er de ikke like
         */
        @Test
        fun `2 like avslag skal være like`() {
            val avslagBoutgifter = AvslagBoutgifter(listOf(ÅrsakAvslag.INGEN_AKTIVITET), "asd")
            val avslagBoutgifter2 = AvslagBoutgifter(listOf(ÅrsakAvslag.INGEN_AKTIVITET), "asd")

            assertThat(avslagBoutgifter).isEqualTo(avslagBoutgifter2)
        }
    }

    @Nested
    inner class OpphørtVedtak {
        val førsteJan25: LocalDate = LocalDate.of(2025, Month.JANUARY, 1)

        @Test
        fun `skal feile om opphørt vedtak ikke har årsaker`() {
            assertThatThrownBy {
                OpphørBoutgifter(
                    beregningsresultat = beregningsresultat(fom = førsteJan25),
                    årsaker = emptyList(),
                    begrunnelse = "begrunnelse",
                    vedtaksperioder =
                        listOf(
                            vedtaksperiode(
                                fom = førsteJan25,
                                tom = førsteJan25,
                            ),
                        ),
                )
            }.hasMessage("Må velge minst en årsak for opphør")
        }

        @Test
        fun `skal feile om opphørt vedtak ikke har begrunnelse for opphør`() {
            assertThatThrownBy {
                OpphørBoutgifter(
                    beregningsresultat = beregningsresultat(fom = førsteJan25),
                    årsaker = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                    begrunnelse = "",
                    vedtaksperioder =
                        listOf(
                            vedtaksperiode(
                                fom = førsteJan25,
                                tom = førsteJan25,
                            ),
                        ),
                )
            }.hasMessage("Opphør må begrunnes")
        }

        /**
         * I tilfelle man setter opp Avslag/sealed class feil så er de ikke like
         */
        @Test
        fun `2 like opphør skal være like`() {
            val avslagBoutgifter =
                OpphørBoutgifter(
                    beregningsresultat = beregningsresultat(fom = førsteJan25),
                    årsaker = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                    begrunnelse = "begrunnelse",
                    vedtaksperioder =
                        listOf(
                            vedtaksperiode(
                                fom = førsteJan25,
                                tom = førsteJan25,
                            ),
                        ),
                )

            val avslagBoutgifter2 =
                OpphørBoutgifter(
                    beregningsresultat = beregningsresultat(fom = førsteJan25),
                    årsaker = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                    begrunnelse = "begrunnelse",
                    vedtaksperioder =
                        listOf(
                            vedtaksperiode(
                                fom = førsteJan25,
                                tom = førsteJan25,
                            ),
                        ),
                )

            assertThat(avslagBoutgifter).isEqualTo(avslagBoutgifter2)
        }
    }
}
