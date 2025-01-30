package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LøpendeMånedTest {
    @Nested
    inner class HarDatoerIUkedager {
        val vedtaksperiodeMandag =
            VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 3))
        val vedtaksperiodeLørdag =
            VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 1))
        val vedtaksperiodeSøndag =
            VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2025, 2, 9), LocalDate.of(2025, 2, 9))
        val vedtaksperiodeHeleMåneden =
            VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28))

        @Test
        fun `løpende måned uten perioder skal gi false`() {
            assertThat(løpendeMåned().harDatoerIUkedager()).isFalse()
        }

        @Test
        fun `skal gi false hvis det kun finnes perioder som er helgdager`() {
            assertThat(løpendeMåned(vedtaksperiodeLørdag).harDatoerIUkedager()).isFalse()
            assertThat(løpendeMåned(vedtaksperiodeLørdag, vedtaksperiodeSøndag).harDatoerIUkedager()).isFalse()
        }

        @Test
        fun `skal gi true hvis det finnes perioder som er ukedager`() {
            assertThat(løpendeMåned(vedtaksperiodeMandag).harDatoerIUkedager()).isTrue()
            assertThat(løpendeMåned(vedtaksperiodeMandag, vedtaksperiodeLørdag).harDatoerIUkedager()).isTrue()
            assertThat(løpendeMåned(vedtaksperiodeHeleMåneden).harDatoerIUkedager()).isTrue()
        }
    }

    private fun løpendeMåned(vararg vedtaksperioder: VedtaksperiodeInnenforLøpendeMåned) =
        LøpendeMåned(
            fom = LocalDate.of(2025, 2, 1),
            tom = LocalDate.of(2025, 2, 28),
            utbetalingsdato = LocalDate.of(2025, 2, 1),
        ).apply {
            vedtaksperioder.forEach {
                this.medVedtaksperiode(it)
            }
        }
}
