package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.grupperVedtaksperioderPerLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LæremidlerPeriodeUtilTest {
    private val FØRSTE_JAN_2024 = LocalDate.of(2024, 1, 1)
    private val SISTE_JAN_2024 = LocalDate.of(2024, 1, 31)
    private val FØRSTE_FEB_2024 = LocalDate.of(2024, 2, 1)
    private val SISTE_FEB_2024 = LocalDate.of(2024, 2, 29)
    private val FØRSTE_MARS_2024 = LocalDate.of(2024, 3, 1)
    private val SISTE_MARS_2024 = LocalDate.of(2024, 3, 31)
    private val SISTE_APRIL_2024 = LocalDate.of(2024, 4, 30)

    @Nested
    inner class SplitPerLøpendeMåneder {
        @Test
        fun `skal splitte periode i løpende måneder 2024-01-01 til 2024-02-01`() {
            val datoperiode = Datoperiode(FØRSTE_JAN_2024, FØRSTE_FEB_2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(FØRSTE_JAN_2024, SISTE_JAN_2024),
                    Datoperiode(FØRSTE_FEB_2024, FØRSTE_FEB_2024),
                )
        }

        @Test
        fun `skal splitte periode i løpende måneder 2024-01-01 til 2024-03-15`() {
            val tom = LocalDate.of(2024, 3, 15)
            val datoperiode = Datoperiode(FØRSTE_JAN_2024, tom)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(FØRSTE_JAN_2024, SISTE_JAN_2024),
                    Datoperiode(FØRSTE_FEB_2024, SISTE_FEB_2024),
                    Datoperiode(FØRSTE_MARS_2024, tom),
                )
        }

        @Test
        fun `fra første dag i måneden`() {
            val datoperiode = Datoperiode(FØRSTE_JAN_2024, SISTE_MARS_2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(FØRSTE_JAN_2024, SISTE_JAN_2024),
                    Datoperiode(FØRSTE_FEB_2024, SISTE_FEB_2024),
                    Datoperiode(FØRSTE_MARS_2024, SISTE_MARS_2024),
                )
        }

        @Test
        fun `fra midt i måneden`() {
            val datoperiode = Datoperiode(LocalDate.of(2024, 1, 15), SISTE_APRIL_2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 14)),
                    Datoperiode(LocalDate.of(2024, 2, 15), LocalDate.of(2024, 3, 14)),
                    Datoperiode(LocalDate.of(2024, 3, 15), LocalDate.of(2024, 4, 14)),
                    Datoperiode(LocalDate.of(2024, 4, 15), SISTE_APRIL_2024),
                )
        }

        // 29 dager i februar i 2024
        @Test
        fun `fra sluttet på måneden`() {
            val datoperiode = Datoperiode(SISTE_JAN_2024, SISTE_APRIL_2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(SISTE_JAN_2024, SISTE_FEB_2024.minusDays(1)),
                    Datoperiode(SISTE_FEB_2024, LocalDate.of(2024, 3, 28)),
                    Datoperiode(LocalDate.of(2024, 3, 29), LocalDate.of(2024, 4, 28)),
                    Datoperiode(LocalDate.of(2024, 4, 29), SISTE_APRIL_2024),
                )
        }

        // 28 dager i februar i 2025
        // må inkludere en dag for februar også for å utbetale et beløp som gjelder for februar og
        @Test
        fun `siste dagen i januar skal løpe til nest siste dagen i februar sånn at man får innvilget for siste dagen i februar og`() {
            val datoperiode = Datoperiode(LocalDate.of(2025, 1, 28), LocalDate.of(2025, 3, 31))
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(LocalDate.of(2025, 1, 28), LocalDate.of(2025, 2, 27)),
                    Datoperiode(LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 27)),
                    Datoperiode(LocalDate.of(2025, 3, 28), LocalDate.of(2025, 3, 31)),
                )
        }

        @Test
        fun `skal håndtere vedtaksperiode som løper over 2 år`() {
            val vedtaksperioder = listOf(
                Vedtaksperiode(LocalDate.of(2024, 12, 5), LocalDate.of(2025, 1, 4)),
            )
            val perioder = vedtaksperioder.grupperVedtaksperioderPerLøpendeMåned()
            assertThat(perioder).hasSize(2)

            with(perioder[0]) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 12, 5))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 12, 31))
                assertThat(this.vedtaksperioder)
                    .containsExactly(Vedtaksperiode(LocalDate.of(2024, 12, 5), LocalDate.of(2024, 12, 31)))
            }
            with(perioder[1]) {
                assertThat(fom).isEqualTo(LocalDate.of(2025, 1, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2025, 1, 31))
                assertThat(this.vedtaksperioder)
                    .containsExactly(Vedtaksperiode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 4)))
            }
        }

        @Nested
        inner class FlereVedtaksperioderSammeMåned {

            @Test
            fun `skal håndtere en vedtaksperiode som løper innenfor en løpende måned`() {
                val vedtaksperioder = listOf(
                    Vedtaksperiode(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 15)),
                )
                val perioder = vedtaksperioder.grupperVedtaksperioderPerLøpendeMåned()
                assertThat(perioder).hasSize(1)
                with(perioder.single()) {
                    assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                    assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                    assertThat(this.vedtaksperioder).hasSize(1)
                }
            }

            @Test
            fun `skal håndtere to vedtaksperiode som løper i ulike løpende måneder`() {
                val vedtaksperioder = listOf(
                    Vedtaksperiode(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 15)),
                    Vedtaksperiode(LocalDate.of(2024, 2, 7), LocalDate.of(2024, 3, 2)),
                )
                val perioder = vedtaksperioder.grupperVedtaksperioderPerLøpendeMåned()
                assertThat(perioder).hasSize(2)
                with(perioder[0]) {
                    assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                    assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                    assertThat(this.vedtaksperioder).hasSize(1)
                }
                with(perioder[1]) {
                    assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 7))
                    assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 6))
                    assertThat(this.vedtaksperioder).hasSize(1)
                }
            }

            @Test
            fun `skal gruppere alle perioder som gjelder januar i januar`() {
                val vedtaksperioder = listOf(
                    Vedtaksperiode(FØRSTE_JAN_2024, LocalDate.of(2024, 1, 5)),
                    Vedtaksperiode(LocalDate.of(2024, 1, 7), LocalDate.of(2024, 1, 7)),
                )
                val perioder = vedtaksperioder.grupperVedtaksperioderPerLøpendeMåned()
                assertThat(perioder).hasSize(1)
                with(perioder.single()) {
                    assertThat(fom).isEqualTo(FØRSTE_JAN_2024)
                    assertThat(tom).isEqualTo(SISTE_JAN_2024)
                    assertThat(this.vedtaksperioder).hasSize(2)
                }
            }

            @Test
            fun `skal gruppere alle perioder som gjelder fra med 5 januar til 4 februar`() {
                val vedtaksperioder = listOf(
                    Vedtaksperiode(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 5)),
                    Vedtaksperiode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 4)),
                )
                val perioder = vedtaksperioder.grupperVedtaksperioderPerLøpendeMåned()
                assertThat(perioder).hasSize(1)
                with(perioder.single()) {
                    assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                    assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                    assertThat(this.vedtaksperioder).hasSize(2)
                }
            }

            @Test
            fun `skal splitte periode 2 som løper over 2 vedtaksperioder`() {
                val vedtaksperioder = listOf(
                    Vedtaksperiode(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 5)),
                    Vedtaksperiode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 28)),
                )
                val perioder = vedtaksperioder.grupperVedtaksperioderPerLøpendeMåned()
                assertThat(perioder).hasSize(2)
                with(perioder[0]) {
                    assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                    assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                    assertThat(this.utbetalingsdato).isEqualTo(LocalDate.of(2024, 1, 5))
                    assertThat(this.vedtaksperioder).hasSize(2)
                }
                with(perioder[1]) {
                    assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 5))
                    assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 4))
                    // Utbetalingsdato for periode 2 blir i neste måned fordi det blir en ny vedtaksperiode
                    assertThat(this.utbetalingsdato).isEqualTo(LocalDate.of(2024, 2, 5))
                    assertThat(this.vedtaksperioder).hasSize(1)
                }
            }
        }

        @Nested
        inner class SammeMåned {

            @Test
            fun `periode som kun gjelder rød dag men ikke helg får innvilget`() {
                val datoperiode = Datoperiode(FØRSTE_JAN_2024, FØRSTE_JAN_2024)
                assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                    .containsExactly(datoperiode)
            }

            // Arena tar ikke med helgdager når man innvilger for lengre perioder, men tar med hvis man kun innvilger en kort periode
            @Test
            fun `periode som gjelder helg får ikke innvilget`() {
                val datoperiode = Datoperiode(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 1, 5))
                assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                    .isEmpty()
            }

            @Test
            fun `skal ikke splitte periode som som har fom = tom`() {
                val datoperiode = Datoperiode(FØRSTE_FEB_2024, FØRSTE_FEB_2024)
                assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                    .containsExactly(datoperiode)
            }

            @Test
            fun `skal ikke splitte periode som er i samme måned`() {
                val datoperiode = Datoperiode(FØRSTE_JAN_2024, SISTE_JAN_2024)
                assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                    .containsExactly(datoperiode)
            }
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
    }
}
