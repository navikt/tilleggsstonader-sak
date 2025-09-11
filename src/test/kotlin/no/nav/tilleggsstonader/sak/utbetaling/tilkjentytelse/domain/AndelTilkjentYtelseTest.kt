package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.YearMonth

class AndelTilkjentYtelseTest {
    val måned = YearMonth.of(2024, 1)

    @Nested
    inner class Dagsandel {
        val andel = {
            andelTilkjentYtelse(
                type = TypeAndel.TILSYN_BARN_AAP,
                satstype = Satstype.DAG,
                fom = måned.atDay(1),
                tom = måned.atDay(1),
            )
        }

        @Test
        fun `fom og tom er samme dag for tilsyn barn`() {
            assertThatCode {
                andel()
            }.doesNotThrowAnyException()
        }

        @Test
        fun `kast feil hvis TOM er før FOM`() {
            assertThatThrownBy {
                andel().copy(
                    fom = måned.atDay(2),
                    tom = måned.atDay(1),
                )
            }.hasMessageContaining("Forventer at fom(2024-01-02) er mindre eller lik tom(2024-01-01)")
        }

        @Test
        fun `kast feil hvis TOM ikke er den samme dagen som FOM for tilsyn barn`() {
            assertThatThrownBy {
                andel().copy(tom = måned.atDay(2))
            }.hasMessageContaining("Forventer at fom(2024-01-01) er lik tom(2024-01-02) for type=TILSYN_BARN_AAP")
        }
    }

    @Nested
    inner class Månedsandel {
        @Disabled // har ingen type som bruker måned ennå så denne kaster exception
        @Test
        fun `fom er første dag i måneden og tom er siste dag i måneden`() {
            assertThatCode {
                andelTilkjentYtelse(
                    type = TypeAndel.LÆREMIDLER_AAP,
                    satstype = Satstype.MÅNED,
                    fom = måned.atDay(1),
                    tom = måned.atEndOfMonth(),
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `kast feil hvis FOM ikke er første dagen i måneden`() {
            assertThatThrownBy {
                andelTilkjentYtelse(
                    type = TypeAndel.LÆREMIDLER_AAP,
                    satstype = Satstype.MÅNED,
                    fom = måned.atDay(2),
                    tom = måned.atEndOfMonth(),
                )
            }.hasMessageContaining("Forventer at fom(2024-01-02) er første dagen i måneden for type=LÆREMIDLER_AAP")
        }

        @Test
        fun `kast feil hvis TOM ikke er siste dagen i måneden`() {
            assertThatThrownBy {
                andelTilkjentYtelse(
                    type = TypeAndel.LÆREMIDLER_AAP,
                    satstype = Satstype.MÅNED,
                    fom = måned.atDay(1),
                    tom = måned.atDay(2),
                )
            }.hasMessageContaining("Forventer at tom(2024-01-02) er siste dagen i måneden for type=LÆREMIDLER_AAP")
        }

        @Test
        fun `kast feil hvis FOM og TOM ikke er i den samme måneden`() {
            assertThatThrownBy {
                andelTilkjentYtelse(
                    type = TypeAndel.LÆREMIDLER_AAP,
                    satstype = Satstype.MÅNED,
                    fom = måned.atDay(1),
                    tom = måned.plusMonths(1).atEndOfMonth(),
                )
            }.hasMessageContaining("Forventer at fom(2024-01-01) og tom(2024-02-29) er i den samme måneden")
        }
    }

    @Nested
    inner class TilsynBarnAndel {
        @Test
        fun `skal kaste feil hvis satstype ikke er dag`() {
            assertThatThrownBy {
                andelTilkjentYtelse(
                    type = TypeAndel.TILSYN_BARN_AAP,
                    satstype = Satstype.MÅNED,
                    fom = måned.atDay(1),
                    tom = måned.atEndOfMonth(),
                )
            }.hasMessageContaining("Forventet satstype=DAG for type=TILSYN_BARN_AAP, men fikk MÅNED")
        }
    }

    @Nested
    inner class LæremidlerAndel {
        @Test
        fun `skal kaste feil hvis satstype ikke er dag`() {
            assertThatThrownBy {
                andelTilkjentYtelse(
                    type = TypeAndel.LÆREMIDLER_AAP,
                    satstype = Satstype.MÅNED,
                    fom = måned.atDay(1),
                    tom = måned.atEndOfMonth(),
                )
            }.hasMessageContaining("Forventet satstype=DAG for type=LÆREMIDLER_AAP, men fikk MÅNED")
        }
    }

    @Nested
    inner class UtbetalingAnnenMåned {
        @Test
        fun `skal ikke kaste feil hvis utbetaling er i annen måned om`() {
            assertDoesNotThrow {
                andelTilkjentYtelse(
                    type = TypeAndel.BOUTGIFTER_AAP,
                    satstype = Satstype.DAG,
                    fom = LocalDate.of(2025, 5, 1),
                    tom = LocalDate.of(2025, 5, 1),
                    utbetalingsdato = LocalDate.of(2025, 6, 2),
                )
            }
        }

        @Test
        fun `skal kaste feil dersom`() {
            val utbetalingsdato = LocalDate.of(2025, 7, 1)
            assertThatThrownBy {
                andelTilkjentYtelse(
                    type = TypeAndel.BOUTGIFTER_AAP,
                    satstype = Satstype.DAG,
                    fom = LocalDate.of(2025, 5, 1),
                    tom = LocalDate.of(2025, 5, 1),
                    utbetalingsdato = utbetalingsdato,
                )
            }.hasMessageContaining("Utbetalingsdato($utbetalingsdato) må være i samme måned")
        }
    }

    @Nested
    inner class DagligReiseAndel {
        @Test
        fun `skal kaste feil hvis satstype ikke er enkeltbeløp`() {
            assertThatThrownBy {
                andelTilkjentYtelse(
                    type = TypeAndel.DAGLIG_REISE_AAP,
                    satstype = Satstype.DAG,
                    fom = måned.atDay(1),
                    tom = måned.atDay(1),
                )
            }.hasMessageContaining("Forventet satstype=ENGANGSBELØP for type=DAGLIG_REISE_AAP, men fikk DAG")
        }
    }
}
