package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.splitVedtaksperiodePerÅr
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerIngenEndringerFørRevurderFra
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class LæremidlerVedtaksperiodeUtilTest {
    private val førsteJan2024 = LocalDate.of(2024, 1, 1)
    private val sisteJan2024 = LocalDate.of(2024, 1, 31)
    private val førsteFeb2024 = LocalDate.of(2024, 2, 1)
    private val sisteFeb2024 = LocalDate.of(2024, 2, 29)
    private val førsteMars2024 = LocalDate.of(2024, 3, 1)
    private val sisteMars2024 = LocalDate.of(2024, 3, 31)
    private val sisteApril2024 = LocalDate.of(2024, 4, 30)
    private val sisteDes2024 = LocalDate.of(2024, 12, 31)

    @Nested
    inner class SplitVedtaksperiodePerÅr {
        @Test
        fun `skal ikke splitte periode som er innenfor samme år`() {
            val periode = Vedtaksperiode(førsteJan2024, sisteDes2024)

            assertThat(listOf(periode).splitVedtaksperiodePerÅr()).containsExactly(
                VedtaksperiodeInnenforÅr(førsteJan2024, sisteDes2024),
            )
        }

        @Test
        fun `skal splitte periode som løper over 2 år`() {
            val periode = Vedtaksperiode(sisteDes2024, sisteDes2024.plusDays(1))

            assertThat(listOf(periode).splitVedtaksperiodePerÅr()).containsExactly(
                VedtaksperiodeInnenforÅr(sisteDes2024, sisteDes2024),
                VedtaksperiodeInnenforÅr(sisteDes2024.plusDays(1), sisteDes2024.plusDays(1)),
            )
        }

        @Test
        fun `skal splitte periode som løper over 3 år`() {
            val periode = Vedtaksperiode(sisteDes2024, LocalDate.of(2026, 2, 3))

            assertThat(listOf(periode).splitVedtaksperiodePerÅr()).containsExactly(
                VedtaksperiodeInnenforÅr(sisteDes2024, sisteDes2024),
                VedtaksperiodeInnenforÅr(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                VedtaksperiodeInnenforÅr(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 3)),
            )
        }
    }

    @Nested
    inner class SplitPerLøpendeMåneder {
        @Test
        fun `skal splitte periode i løpende måneder 2024-01-01 til 2024-02-01`() {
            val datoperiode = Datoperiode(førsteJan2024, førsteFeb2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(førsteJan2024, sisteJan2024),
                    Datoperiode(førsteFeb2024, førsteFeb2024),
                )
        }

        @Test
        fun `skal splitte periode i løpende måneder 2024-01-01 til 2024-03-15`() {
            val tom = LocalDate.of(2024, 3, 15)
            val datoperiode = Datoperiode(førsteJan2024, tom)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(førsteJan2024, sisteJan2024),
                    Datoperiode(førsteFeb2024, sisteFeb2024),
                    Datoperiode(førsteMars2024, tom),
                )
        }

        @Test
        fun `fra første dag i måneden`() {
            val datoperiode = Datoperiode(førsteJan2024, sisteMars2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(førsteJan2024, sisteJan2024),
                    Datoperiode(førsteFeb2024, sisteFeb2024),
                    Datoperiode(førsteMars2024, sisteMars2024),
                )
        }

        @Test
        fun `fra midt i måneden`() {
            val datoperiode = Datoperiode(LocalDate.of(2024, 1, 15), sisteApril2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 14)),
                    Datoperiode(LocalDate.of(2024, 2, 15), LocalDate.of(2024, 3, 14)),
                    Datoperiode(LocalDate.of(2024, 3, 15), LocalDate.of(2024, 4, 14)),
                    Datoperiode(LocalDate.of(2024, 4, 15), sisteApril2024),
                )
        }

        // 29 dager i februar i 2024
        @Test
        fun `fra sluttet på måneden`() {
            val datoperiode = Datoperiode(sisteJan2024, sisteApril2024)
            assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                .containsExactly(
                    Datoperiode(sisteJan2024, sisteFeb2024.minusDays(1)),
                    Datoperiode(sisteFeb2024, LocalDate.of(2024, 3, 28)),
                    Datoperiode(LocalDate.of(2024, 3, 29), LocalDate.of(2024, 4, 28)),
                    Datoperiode(LocalDate.of(2024, 4, 29), sisteApril2024),
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

        @Nested
        inner class SammeMåned {
            @Test
            fun `periode som kun gjelder rød dag men ikke helg får innvilget`() {
                val datoperiode = Datoperiode(førsteJan2024, førsteJan2024)
                assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                    .containsExactly(
                        datoperiode,
                    )
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
                val datoperiode = Datoperiode(førsteFeb2024, førsteFeb2024)
                assertThat(datoperiode.splitPerLøpendeMåneder { fom, tom -> Datoperiode(fom = fom, tom = tom) })
                    .containsExactly(datoperiode)
            }

            @Test
            fun `skal ikke splitte periode som er i samme måned`() {
                val datoperiode = Datoperiode(førsteJan2024, sisteJan2024)
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

    @Nested
    inner class ValiderIngenEndringerFørRevurderFra {
        val vedtaksperiodeJanFeb =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            )

        val vedtaksperiodeMars =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 3, 1),
                tom = LocalDate.of(2025, 3, 31),
            )

        val vedtaksperiodeApril =
            Vedtaksperiode(
                fom = LocalDate.of(2025, 4, 1),
                tom = LocalDate.of(2025, 4, 30),
            )

        val vedtaksperioderJanFeb = listOf(vedtaksperiodeJanFeb)
        val vedtaksperioderJanMars = listOf(vedtaksperiodeJanFeb, vedtaksperiodeMars)
        val førsteMars: LocalDate = LocalDate.of(2025, 3, 1)
        val femtendeMars: LocalDate = LocalDate.of(2025, 3, 15)
        val førsteApril: LocalDate = LocalDate.of(2025, 4, 1)

        @Test
        fun `kaster ikke feil ved ingen revurder fra og ingen gamle perioder (førstegangsbehandling)`() {
            assertDoesNotThrow { validerIngenEndringerFørRevurderFra(vedtaksperioderJanMars, emptyList(), null) }
        }

        @Test
        fun `kaster ikke feil ved ny periode som starter etter revurder fra`() {
            assertDoesNotThrow {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioderJanMars,
                    vedtaksperioderJanFeb,
                    førsteMars,
                )
            }
        }

        @Test
        fun `kaster feil ved ny periode med fom før revurder fra`() {
            assertThrows<ApiFeil> {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioderJanMars,
                    vedtaksperioderJanFeb,
                    femtendeMars,
                )
            }
        }

        @Test
        fun `kaster feil ved ny periode med fom og tom før revuder fra`() {
            assertThrows<ApiFeil> {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioderJanMars,
                    vedtaksperioderJanFeb,
                    førsteApril,
                )
            }
        }

        @Test
        fun `kaster feil ved ny lik som er lik eksisterende periode lagt til før revuder fra`() {
            val nyeVedtaksperioder =
                listOf(
                    vedtaksperiodeJanFeb,
                    vedtaksperiodeJanFeb,
                )

            assertThrows<ApiFeil> {
                validerIngenEndringerFørRevurderFra(
                    nyeVedtaksperioder,
                    vedtaksperioderJanFeb,
                    førsteMars,
                )
            }
        }

        @Test
        fun `kaster feil ved tom flyttet til før revurder fra`() {
            val nyeVedtaksperioder =
                listOf(
                    vedtaksperiodeJanFeb,
                    vedtaksperiodeMars.copy(tom = LocalDate.of(2025, 3, 10)),
                )

            assertThrows<ApiFeil> {
                validerIngenEndringerFørRevurderFra(
                    nyeVedtaksperioder,
                    vedtaksperioderJanMars,
                    femtendeMars,
                )
            }
        }

        @Test
        fun `kaster feil ved fom og tom flyttet til før revurder fra`() {
            val gamleVedtaksperioder =
                listOf(
                    vedtaksperiodeJanFeb,
                    vedtaksperiodeApril,
                )

            assertThrows<ApiFeil> {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioderJanMars,
                    gamleVedtaksperioder,
                    førsteApril,
                )
            }
        }

        @Test
        fun `kaster ikke feil ved slettet perioder etter revurder fra`() {
            assertDoesNotThrow {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioderJanFeb,
                    vedtaksperioderJanMars,
                    førsteMars,
                )
            }
        }

        @Test
        fun `kaster feil ved slettet periode med fom før revurder fra`() {
            assertThrows<ApiFeil> {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioderJanFeb,
                    vedtaksperioderJanMars,
                    femtendeMars,
                )
            }
        }

        @Test
        fun `kaster feil ved slettet periode med fom og tom før revurder fra`() {
            assertThrows<ApiFeil> {
                validerIngenEndringerFørRevurderFra(
                    vedtaksperioderJanFeb,
                    vedtaksperioderJanMars,
                    førsteApril,
                )
            }
        }
    }
}
