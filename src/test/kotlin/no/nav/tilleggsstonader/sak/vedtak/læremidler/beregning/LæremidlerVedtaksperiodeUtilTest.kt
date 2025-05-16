package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.splitVedtaksperiodePerÅr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
}
