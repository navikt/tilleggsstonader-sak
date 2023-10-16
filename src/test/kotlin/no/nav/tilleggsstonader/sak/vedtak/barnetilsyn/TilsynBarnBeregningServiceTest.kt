package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

class TilsynBarnBeregningServiceTest {

    val service = TilsynBarnBeregningService()

    val barnId = UUID.randomUUID()

    val jan = YearMonth.of(2023, 1)
    val feb = YearMonth.of(2023, 2)
    val mars = YearMonth.of(2023, 3)

    @Nested
    inner class ValideringStønadsperioder {

        @Test
        fun `skal validere for at det finnes perioder`() {
            assertThatThrownBy {
                service.beregn(emptyList(), emptyMap())
            }.hasMessage("Stønadsperioder mangler")
        }

        @Test
        fun `skal validere sorterte perioder`() {
            val stønadsperioder = listOf(
                Stønadsperiode(feb.atDay(1), feb.atEndOfMonth()),
                Stønadsperiode(jan.atDay(1), jan.atEndOfMonth()),
            )
            assertThatThrownBy {
                service.beregn(stønadsperioder, emptyMap())
            }.hasMessage("Stønadsperioder er ikke sortert")
        }

        @Test
        fun `skal validere overlappende perioder`() {
            val stønadsperioder = listOf(
                Stønadsperiode(jan.atDay(1), feb.atEndOfMonth()),
                Stønadsperiode(feb.atDay(1), feb.atEndOfMonth()),
            )
            assertThatThrownBy {
                service.beregn(stønadsperioder, emptyMap())
            }.hasMessage("Stønadsperioder overlapper")
        }
    }

    @Nested
    inner class ValideringUtgifter {

        val stønadsperioder = listOf(Stønadsperiode(jan.atDay(1), feb.atEndOfMonth()))

        @Test
        fun `skal validere for at det finnes perioder`() {
            assertThatThrownBy {
                service.beregn(stønadsperioder, emptyMap())
            }.hasMessage("Utgiftsperioder mangler")

            assertThatThrownBy {
                service.beregn(stønadsperioder, mapOf(barnId to emptyList()))
            }.hasMessage("Utgiftsperioder mangler")
        }

        @Test
        fun `skal validere sorterte perioder`() {
            val utgifter = mapOf<UUID, List<Utgift>>(
                barnId to listOf(
                    Utgift(feb, feb, 100),
                    Utgift(jan, jan, 100)
                )
            )
            assertThatThrownBy {
                service.beregn(stønadsperioder, utgifter)
            }.hasMessage("Utgiftsperioder er ikke sortert")
        }

        @Test
        fun `skal validere overlappende perioder`() {
            val utgifter = mapOf<UUID, List<Utgift>>(
                barnId to listOf(
                    Utgift(jan, mars, 100),
                    Utgift(feb, feb, 100),
                )
            )
            assertThatThrownBy {
                service.beregn(stønadsperioder, utgifter)
            }.hasMessage("Utgiftsperioder overlapper")
        }
    }

}