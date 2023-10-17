package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Mergeable
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Periode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.erSortert
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.overlapper
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.splitPerMåned
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class PeriodeTest {

    val jan = YearMonth.of(2023, 1)
    val feb = YearMonth.of(2023, 2)
    val mars = YearMonth.of(2023, 3)
    val april = YearMonth.of(2023, 4)
    val mai = YearMonth.of(2023, 5)

    @Nested
    inner class ErSortert {

        @Test
        fun `kun en periode er sortert`() {
            val periode1 = Månedsperiode(jan, mars)
            assertThat(listOf(periode1).erSortert()).isTrue
        }

        @Test
        fun `periode 2 starter etter periode 2`() {
            val periode1 = Månedsperiode(jan, jan)
            val periode2 = Månedsperiode(feb, feb)
            assertThat(listOf(periode1, periode2).erSortert()).isTrue
        }

        @Test
        fun `periode 2 starter før periode 1`() {
            val periode1 = Månedsperiode(feb, feb)
            val periode2 = Månedsperiode(jan, jan)
            assertThat(listOf(periode1, periode2).erSortert()).isFalse
        }
    }

    @Nested
    inner class Overlapper {
        @Test
        fun `periode 2 starter i samme måned som periode 1 slutter`() {
            val periode1 = Månedsperiode(jan, mars)
            val periode2 = Månedsperiode(mars, april)
            assertThat(listOf(periode1, periode2).overlapper()).isTrue
        }

        @Test
        fun `periode 2 starter i midten på periode 1`() {
            val periode1 = Månedsperiode(jan, mars)
            val periode2 = Månedsperiode(feb, april)
            assertThat(listOf(periode1, periode2).overlapper()).isTrue
        }

        @Test
        fun `periode 2 er en del av periode 1`() {
            val periode1 = Månedsperiode(jan, mars)
            val periode2 = Månedsperiode(feb, feb)
            assertThat(listOf(periode1, periode2).overlapper()).isTrue
        }

        @Test
        fun `periode 2 er etter periode 1`() {
            val periode1 = Månedsperiode(jan, feb)
            val periode2 = Månedsperiode(mars, april)
            assertThat(listOf(periode1, periode2).overlapper()).isFalse
        }

        @Test
        fun `periode 2 er før periode 1`() {
            val periode1 = Månedsperiode(feb, feb)
            val periode2 = Månedsperiode(jan, jan)
            assertThat(listOf(periode1, periode2).overlapper()).isFalse
        }
    }

    @Nested
    inner class MergeSammenhengende {
        private val periodeJan = Månedsperiode(jan, jan, 1)
        private val periodeFeb = Månedsperiode(feb, feb, 1)
        private val periodeMars = Månedsperiode(mars, mars, 2)
        private val periodeMai = Månedsperiode(mai, mai, 2)

        @Test
        fun `skal slå sammen perioder som er sammenhengende og har samme verdi`() {
            assertThat(listOf(periodeJan, periodeFeb).mergeSammenhengende(::skalMerges))
                .containsExactly(periodeJan.copy(tom = feb))
        }

        @Test
        fun `perioder med ulikt beløp skal ikke slås sammen`() {
            assertThat(listOf(periodeFeb, periodeMars).mergeSammenhengende(::skalMerges))
                .containsExactly(periodeFeb, periodeMars)
        }

        @Test
        fun `perioder som ikke er sammenhengende skal ikke slås sammen`() {
            assertThat(listOf(periodeMars, periodeMai).mergeSammenhengende(::skalMerges))
                .containsExactly(periodeMars, periodeMai)
        }

        private fun skalMerges(
            m: Månedsperiode,
            m2: Månedsperiode
        ) = m.tom.plusMonths(1) == m2.fom && m.verdi == m2.verdi
    }

    @Nested
    inner class SplitPerMåned {

        private val verdi = 1

        private val datoperiode = Datoperiode(jan.atDay(5), feb.atEndOfMonth(), verdi)

        private val månedsperiode1 = Månedsperiode(jan, jan, verdi)
        private val månedsperiode2 = Månedsperiode(jan, mars, 10)

        @Test
        fun `skal splitte datoperiode per måned`() {
            assertThat(datoperiode.splitPerMåned { it.verdi })
                .containsExactly(Pair(jan, verdi), Pair(feb, verdi))
        }

        @Test
        fun `skal splitte månedsperiode per måned`() {
            assertThat(månedsperiode1.splitPerMåned { it.verdi })
                .containsExactly(Pair(jan, verdi))

            assertThat(månedsperiode2.splitPerMåned { it.verdi })
                .containsExactly(Pair(jan, 10), Pair(feb, 10), Pair(mars, 10))
        }
    }

    private data class Datoperiode(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val verdi: Int = 0,
    ) : Periode<LocalDate>

    private data class Månedsperiode(
        override val fom: YearMonth,
        override val tom: YearMonth,
        val verdi: Int = 0,
    ) : Periode<YearMonth>, Mergeable<YearMonth, Månedsperiode> {
        override fun merge(other: Månedsperiode): Månedsperiode {
            return this.copy(tom = other.tom)
        }
    }
}
