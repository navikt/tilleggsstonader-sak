package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Periode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.erSortert
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.overlapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class PeriodeTest {

    data class MinPeriode(
        override val fom: YearMonth,
        override val tom: YearMonth
    ) : Periode<YearMonth>

    val jan = YearMonth.of(2023, 1)
    val feb = YearMonth.of(2023, 2)
    val mars = YearMonth.of(2023, 3)
    val april = YearMonth.of(2023, 4)

    @Nested
    inner class ErSortert {

        @Test
        fun `kun en periode er sortert`() {
            val periode1 = MinPeriode(jan, mars)
            assertThat(listOf(periode1).erSortert()).isTrue
        }

        @Test
        fun `periode 2 starter etter periode 2`() {
            val periode1 = MinPeriode(jan, jan)
            val periode2 = MinPeriode(feb, feb)
            assertThat(listOf(periode1, periode2).erSortert()).isTrue
        }

        @Test
        fun `periode 2 starter før periode 1`() {
            val periode1 = MinPeriode(feb, feb)
            val periode2 = MinPeriode(jan, jan)
            assertThat(listOf(periode1, periode2).erSortert()).isFalse
        }

    }

    @Nested
    inner class Overlapper {
        @Test
        fun `periode 2 starter i samme måned som periode 1 slutter`() {
            val periode1 = MinPeriode(jan, mars)
            val periode2 = MinPeriode(mars, april)
            assertThat(listOf(periode1, periode2).overlapper()).isTrue
        }

        @Test
        fun `periode 2 starter i midten på periode 1`() {
            val periode1 = MinPeriode(jan, mars)
            val periode2 = MinPeriode(feb, april)
            assertThat(listOf(periode1, periode2).overlapper()).isTrue
        }

        @Test
        fun `periode 2 er en del av periode 1`() {
            val periode1 = MinPeriode(jan, mars)
            val periode2 = MinPeriode(feb, feb)
            assertThat(listOf(periode1, periode2).overlapper()).isTrue
        }

        @Test
        fun `periode 2 er etter periode 1`() {
            val periode1 = MinPeriode(jan, feb)
            val periode2 = MinPeriode(mars, april)
            assertThat(listOf(periode1, periode2).overlapper()).isFalse
        }

        @Test
        fun `periode 2 er før periode 1`() {
            val periode1 = MinPeriode(feb, feb)
            val periode2 = MinPeriode(jan, jan)
            assertThat(listOf(periode1, periode2).overlapper()).isFalse
        }
    }

}
