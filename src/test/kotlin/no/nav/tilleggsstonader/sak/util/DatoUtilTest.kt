package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class DatoUtilTest {
    @Test
    fun `antallÅrSiden skal returnere 0 hvis vi sender inn dagens dato`() {
        assertThat(antallÅrSiden(LocalDate.now())).isEqualTo(0)
    }

    @Test
    fun `antallÅrSiden skal returnere null hvis vi sender inn null`() {
        assertThat(antallÅrSiden(null)).isNull()
    }

    @Test
    fun `antallÅrSiden skal returnere 0 hvis vi sender inn morgendagens dato for ett år siden`() {
        val morgendagensDatoIFjor = LocalDate.now().minusYears(1).plusDays(1)
        assertThat(antallÅrSiden(morgendagensDatoIFjor)).isEqualTo(0)
    }

    @Test
    fun `antallÅrSiden skal returnere 1 hvis vi sender inn gårsdagens dato for ett år siden`() {
        val gårsdagensDatoIFjor = LocalDate.now().minusYears(1).minusDays(1)
        assertThat(antallÅrSiden(gårsdagensDatoIFjor)).isEqualTo(1)
    }

    @Test
    fun `nesteMandagHvisHelg skal håndtere dagens dato hvis lørdag eller søndag`() {
        val april = YearMonth.of(2024, 4)
        listOf(
            april.atDay(1) to april.atDay(1),
            april.atDay(2) to april.atDay(2),
            april.atDay(3) to april.atDay(3),
            april.atDay(4) to april.atDay(4),
            april.atDay(5) to april.atDay(5),
            april.atDay(6) to april.atDay(8),
            april.atDay(7) to april.atDay(8),
        ).forEach {
            assertThat(it.first.datoEllerNesteMandagHvisLørdagEllerSøndag()).isEqualTo(it.second)
        }
    }

    @Nested
    inner class FørsteDagIMpneden {
        @Test
        fun `erFørsteDagIMåneden skal sjekke om dagen er siste dag i måneden`() {
            assertThat(LocalDate.of(2024, 1, 1).erFørsteDagIMåneden()).isTrue
            assertThat(LocalDate.of(2024, 2, 1).erFørsteDagIMåneden()).isTrue

            assertThat(LocalDate.of(2024, 1, 31).erFørsteDagIMåneden()).isFalse
            assertThat(LocalDate.of(2024, 3, 30).erFørsteDagIMåneden()).isFalse
        }
    }

    @Nested
    inner class FørsteDagIMåneden {
        @Test
        fun `tilFørsteDagIMåneden skal endre dato til første dagen i måneden for inneværende måned`() {
            val førsteJan2024 = LocalDate.of(2024, 1, 1)
            assertThat(førsteJan2024.tilFørsteDagIMåneden()).isEqualTo(førsteJan2024)
            assertThat(LocalDate.of(2024, 1, 31).tilFørsteDagIMåneden()).isEqualTo(førsteJan2024)
            assertThat(LocalDate.of(2024, 1, 30).tilFørsteDagIMåneden()).isEqualTo(førsteJan2024)
            assertThat(LocalDate.of(2024, 2, 4).tilFørsteDagIMåneden()).isEqualTo(LocalDate.of(2024, 2, 1))
        }
    }

    @Nested
    inner class SisteDagIMåneden {
        @Test
        fun `erSisteDagIMåneden skal sjekke om dagen er siste dag i måneden`() {
            assertThat(LocalDate.of(2024, 1, 31).erSisteDagIMåneden()).isTrue
            assertThat(LocalDate.of(2024, 2, 1).erSisteDagIMåneden()).isFalse
            assertThat(LocalDate.of(2024, 3, 30).erSisteDagIMåneden()).isFalse
        }

        @Test
        fun `tilSisteDagIMåneden skal endre dato til siste dagen i måneden for inneværende måned`() {
            assertThat(LocalDate.of(2024, 1, 31).tilSisteDagIMåneden()).isEqualTo(LocalDate.of(2024, 1, 31))
            assertThat(LocalDate.of(2024, 1, 30).tilSisteDagIMåneden()).isEqualTo(LocalDate.of(2024, 1, 31))
            assertThat(LocalDate.of(2024, 2, 4).tilSisteDagIMåneden()).isEqualTo(LocalDate.of(2024, 2, 29))
        }
    }

    @Nested
    inner class TilSisteDagenIÅret {
        @Test
        fun `tilSisteDagenIÅret skal gi siste dagen i året`() {
            assertThat(LocalDate.of(2024, 1, 12).tilSisteDagenIÅret()).isEqualTo(LocalDate.of(2024, 12, 31))
            assertThat(LocalDate.of(2025, 12, 31).tilSisteDagenIÅret()).isEqualTo(LocalDate.of(2025, 12, 31))
        }
    }

    @Nested
    inner class LørdagEllerSøndag {
        @Test
        fun `ukesdager skal gi false`() {
            assertThat(LocalDate.of(2025, 1, 1).lørdagEllerSøndag()).isFalse() // onsdag rød dag
            assertThat(LocalDate.of(2025, 1, 2).lørdagEllerSøndag()).isFalse() // torsdag
            assertThat(LocalDate.of(2025, 1, 3).lørdagEllerSøndag()).isFalse() // fredag

            assertThat(LocalDate.of(2025, 2, 3).lørdagEllerSøndag()).isFalse() // mandag
            assertThat(LocalDate.of(2025, 2, 4).lørdagEllerSøndag()).isFalse() // tirsdag
        }

        @Test
        fun `lørdag og søndag skal gi true`() {
            assertThat(LocalDate.of(2025, 1, 4).lørdagEllerSøndag()).isTrue() // lørdag
            assertThat(LocalDate.of(2025, 1, 5).lørdagEllerSøndag()).isTrue() // søndag
        }
    }

    @Nested
    inner class InneholderUkedag {
        @Test
        fun `periode som kun inneholder helg skal gi false`() {
            val datoperiode1 = Datoperiode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 1, 5))
            val datoperiode2 = Datoperiode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 1, 5))

            assertThat(datoperiode1.inneholderUkedag()).isFalse
            assertThat(datoperiode2.inneholderUkedag()).isFalse
        }

        @Test
        fun `periode som kun inneholder en ukesdag skal gi true`() {
            val kunUkedag = Datoperiode(LocalDate.of(2025, 1, 3), LocalDate.of(2025, 1, 3))
            assertThat(kunUkedag.inneholderUkedag()).isTrue
        }

        @Test
        fun `periode som kun en ukesdag og en helgdag skal gi true`() {
            val ukedagOgHelg = Datoperiode(LocalDate.of(2025, 1, 3), LocalDate.of(2025, 1, 4))

            assertThat(ukedagOgHelg.inneholderUkedag()).isTrue
        }

        @Test
        fun `periode som kun inneholder en ukesdag som er rød dag skal gi true`() {
            val rødDag = Datoperiode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1))

            assertThat(rødDag.inneholderUkedag()).isTrue
        }

        @Test
        fun `periode som går fra søndag til søndag en annen uke skal gi true`() {
            val rødDag = Datoperiode(LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 12))

            assertThat(rødDag.inneholderUkedag()).isTrue
        }
    }

    @Nested
    inner class SisteDagenILøpendeMåned {
        @Test
        fun `skal finne dato i neste måned`() {
            assertThat(LocalDate.of(2025, 1, 1).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 1, 31))
            assertThat(LocalDate.of(2025, 2, 1).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 2, 28))
            assertThat(LocalDate.of(2025, 3, 1).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 3, 31))
            assertThat(LocalDate.of(2025, 4, 1).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 4, 30))
        }

        @Test
        fun `hvis neste måned har færre antall dager`() {
            assertThat(LocalDate.of(2025, 2, 28).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 3, 27))
            assertThat(LocalDate.of(2025, 4, 30).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 5, 29))
        }

        @Test
        fun `hvis dagens måned har flere dager enn neste skal man bruke siste dagen i måneden`() {
            assertThat(LocalDate.of(2025, 1, 29).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 2, 27))
            assertThat(LocalDate.of(2025, 1, 30).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 2, 27))
            assertThat(LocalDate.of(2025, 1, 31).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 2, 27))

            assertThat(LocalDate.of(2025, 3, 31).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2025, 4, 29))
        }

        @Nested
        inner class Skuddår {
            @Test
            fun `skal finne dato i neste måned`() {
                assertThat(LocalDate.of(2024, 1, 1).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2024, 1, 31))
                assertThat(LocalDate.of(2024, 2, 1).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2024, 2, 29))
            }

            @Test
            fun `hvis neste måned har færre antall dager`() {
                assertThat(LocalDate.of(2024, 2, 29).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2024, 3, 28))
                assertThat(LocalDate.of(2024, 4, 30).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2024, 5, 29))
            }

            @Test
            fun `hvis dagens måned har flere dager enn neste skal man bruke siste dagen i måneden`() {
                assertThat(LocalDate.of(2024, 1, 29).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2024, 2, 28))
                assertThat(LocalDate.of(2024, 1, 30).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2024, 2, 28))
                assertThat(LocalDate.of(2024, 1, 31).sisteDagenILøpendeMåned()).isEqualTo(LocalDate.of(2024, 2, 28))
            }
        }

        @Nested
        inner class ValiderUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode {
            @Test
            fun `skal ikke kaste feil når delperioder er mandag til søndag`() {
                val overordnetPeriode = Datoperiode(5 januar 2026, 1 februar 2026)
                val delperioder =
                    listOf(
                        Datoperiode(5 januar 2026, 11 januar 2026),
                        Datoperiode(12 januar 2026, 18 januar 2026),
                        Datoperiode(19 januar 2026, 25 januar 2026),
                        Datoperiode(26 januar 2026, 1 februar 2026),
                    )

                validerUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode(overordnetPeriode, delperioder)
            }

            @Test
            fun `skal ikke kaste feil når delperiodene er ukentlige og sammenhengende`() {
                val overordnetPeriode = Datoperiode(7 januar 2026, 29 januar 2026)
                val delperioder =
                    listOf(
                        Datoperiode(7 januar 2026, 11 januar 2026),
                        Datoperiode(12 januar 2026, 18 januar 2026),
                        Datoperiode(19 januar 2026, 25 januar 2026),
                        Datoperiode(26 januar 2026, 29 januar 2026),
                    )

                validerUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode(overordnetPeriode, delperioder)
            }

            @Test
            fun `skal kaste feil ved overlappende delperioder`() {
                val overordnetPeriode = Datoperiode(5 januar 2026, 18 januar 2026)
                val delperioder =
                    listOf(
                        Datoperiode(5 januar 2026, 12 januar 2026),
                        Datoperiode(12 januar 2026, 18 januar 2026),
                    )

                val feil =
                    assertThrows<ApiFeil> {
                        validerUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode(
                            overordnetPeriode,
                            delperioder,
                        )
                    }

                assertThat(feil.message).contains("Delperioder kan ikke overlappe")
            }

            @Test
            fun `skal kaste feil ved opphold mellom delperioder`() {
                val overordnetPeriode = Datoperiode(5 januar 2026, 19 januar 2026)
                val delperioder =
                    listOf(
                        Datoperiode(5 januar 2026, 11 januar 2026),
                        Datoperiode(13 januar 2026, 19 januar 2026),
                    )

                val feil =
                    assertThrows<ApiFeil> {
                        validerUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode(
                            overordnetPeriode,
                            delperioder,
                        )
                    }

                assertThat(feil.message).contains("Det er opphold mellom delperiodene")
            }

            @Test
            fun `skal kaste feil når en mellomperiode ikke slutter på søndag`() {
                val overordnetPeriode = Datoperiode(5 januar 2026, 25 januar 2026)
                val delperioder =
                    listOf(
                        Datoperiode(5 januar 2026, 11 januar 2026),
                        Datoperiode(12 januar 2026, 17 januar 2026),
                        Datoperiode(18 januar 2026, 25 januar 2026),
                    )

                val feil =
                    assertThrows<ApiFeil> {
                        validerUkentligeDelperioderErSammenhengendeInnenforOverordnetPeriode(
                            overordnetPeriode,
                            delperioder,
                        )
                    }

                assertThat(feil.message).contains("må slutte på en søndag")
            }
        }
    }
}
