package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.splitVedtaksperiodePerÅr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LæremidlerVedtaksperiodeUtilTest {
    private val førsteJan2024 = LocalDate.of(2024, 1, 1)
    private val sisteDes2024 = LocalDate.of(2024, 12, 31)

    @Nested
    inner class SplitVedtaksperiodePerÅr {
        @Test
        fun `skal ikke splitte periode som er innenfor samme år`() {
            val periode = vedtaksperiodeBeregning(fom = førsteJan2024, tom = sisteDes2024)

            assertThat(listOf(periode).splitVedtaksperiodePerÅr()).containsExactly(
                VedtaksperiodeInnenforÅr(førsteJan2024, sisteDes2024),
            )
        }

        @Test
        fun `skal splitte periode som løper over 2 år`() {
            val periode = vedtaksperiodeBeregning(fom = sisteDes2024, tom = sisteDes2024.plusDays(1))
            assertThat(listOf(periode).splitVedtaksperiodePerÅr()).containsExactly(
                VedtaksperiodeInnenforÅr(sisteDes2024, sisteDes2024),
                VedtaksperiodeInnenforÅr(sisteDes2024.plusDays(1), sisteDes2024.plusDays(1)),
            )
        }

        @Test
        fun `skal splitte periode som løper over 3 år`() {
            val periode = vedtaksperiodeBeregning(fom = sisteDes2024, tom = LocalDate.of(2026, 2, 3))

            assertThat(listOf(periode).splitVedtaksperiodePerÅr()).containsExactly(
                VedtaksperiodeInnenforÅr(sisteDes2024, sisteDes2024),
                VedtaksperiodeInnenforÅr(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                VedtaksperiodeInnenforÅr(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 3)),
            )
        }
    }
}
