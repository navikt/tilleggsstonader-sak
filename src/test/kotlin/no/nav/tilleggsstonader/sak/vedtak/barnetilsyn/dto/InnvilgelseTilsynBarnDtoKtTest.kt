package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class InnvilgelseTilsynBarnDtoKtTest {
    @Test
    fun `skal mappe til dto`() {
        val dto =
            BeregningsresultatTilsynBarn(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            vedtaksperiodeGrunnlag =
                                listOf(
                                    vedtaksperiodeGrunnlag(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4)),
                                ),
                            beløpsperioder = listOf(Beløpsperiode(LocalDate.of(2024, 1, 1), 20, NEDSATT_ARBEIDSEVNE)),
                        ),
                    ),
            ).tilDto(null)

        assertThat(dto.perioder).containsExactlyInAnyOrder(
            BeregningsresultatForMånedDto(
                dagsats = 10.toBigDecimal(),
                grunnlag =
                    BeregningsgrunnlagDto(
                        måned = YearMonth.of(2024, 1),
                        utgifterTotal = 100,
                        antallBarn = 4,
                    ),
                månedsbeløp = 20,
            ),
        )
    }

    @Test
    fun `gjelder fra og til skal mappes fra min og maks ax vedtaksperioder`() {
        val dto =
            BeregningsresultatTilsynBarn(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            vedtaksperiodeGrunnlag =
                                listOf(
                                    vedtaksperiodeGrunnlag(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4)),
                                    vedtaksperiodeGrunnlag(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16)),
                                ),
                        ),
                        beregningsresultatForMåned(
                            vedtaksperiodeGrunnlag =
                                listOf(
                                    vedtaksperiodeGrunnlag(LocalDate.of(2024, 2, 3), LocalDate.of(2024, 2, 4)),
                                ),
                        ),
                    ),
            ).tilDto(null)

        assertThat(dto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 2))
        assertThat(dto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 2, 4))
    }

    @Nested
    inner class TidligsteEndringMånedsbeløp {
        /*
         * skal mappe beløp fra perioder som gjelder fra og med tidligste endring då det ikke er ønskelig å vise
         * beløp som allerede er innvilget
         */
        @Test
        fun `skal mappe beløp fra perioder som gjelder fra og med tidligste endring`() {
            val tidligsteEndring = LocalDate.of(2024, 1, 17)
            val dto =
                BeregningsresultatTilsynBarn(
                    perioder =
                        listOf(
                            beregningsresultatForMåned(
                                beløpsperioder =
                                    listOf(
                                        Beløpsperiode(tidligsteEndring.minusDays(1), 10, NEDSATT_ARBEIDSEVNE),
                                        Beløpsperiode(tidligsteEndring, 20, NEDSATT_ARBEIDSEVNE),
                                        Beløpsperiode(tidligsteEndring.plusDays(1), 30, NEDSATT_ARBEIDSEVNE),
                                    ),
                            ),
                        ),
                ).tilDto(tidligsteEndring)

            assertThat(dto.perioder.single().månedsbeløp).isEqualTo(50)
        }
    }

    @Nested
    inner class TidligsteEndringGjelderFraOgTil {
        val tidligsteEndring = LocalDate.of(2024, 1, 17)

        @Test
        fun `periode som overlapper skal bruke tidligsteEndring som startdato`() {
            val periode = vedtaksperiodeGrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 18))
            val dto = resultatMedEnVedtaksperiode(periode).tilDto(tidligsteEndring)

            assertThat(dto.gjelderFraOgMed).isEqualTo(tidligsteEndring)
            assertThat(dto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 18))
        }

        @Test
        fun `periode som begynner før tidligsteEndring skal ikke brukes til gjelderFra eller gjelderTil`() {
            val periode = vedtaksperiodeGrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 16))
            val dto = resultatMedEnVedtaksperiode(periode).tilDto(tidligsteEndring)

            assertThat(dto.gjelderFraOgMed).isNull()
            assertThat(dto.gjelderTilOgMed).isNull()
        }

        @Test
        fun `periode som begynner fra og med tidligsteEndring brukes til gjelderFra og gjelderTil`() {
            val periode = vedtaksperiodeGrunnlag(fom = LocalDate.of(2024, 1, 17), tom = LocalDate.of(2024, 1, 19))
            val dto = resultatMedEnVedtaksperiode(periode).tilDto(tidligsteEndring)

            assertThat(dto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 17))
            assertThat(dto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 19))
        }

        private fun resultatMedEnVedtaksperiode(vedtaksperiodeGrunnlag: VedtaksperiodeGrunnlag) =
            BeregningsresultatTilsynBarn(
                perioder =
                    listOf(
                        beregningsresultatForMåned(vedtaksperiodeGrunnlag = listOf(vedtaksperiodeGrunnlag)),
                    ),
            )
    }

    private fun beregningsresultatForMåned(
        vedtaksperiodeGrunnlag: List<VedtaksperiodeGrunnlag> = emptyList(),
        beløpsperioder: List<Beløpsperiode> = emptyList(),
    ) = BeregningsresultatForMåned(
        dagsats = 10.toBigDecimal(),
        månedsbeløp = 100,
        grunnlag =
            Beregningsgrunnlag(
                måned = YearMonth.of(2024, 1),
                makssats = 1000,
                vedtaksperiodeGrunnlag = vedtaksperiodeGrunnlag,
                utgifter = emptyList(),
                antallBarn = 4,
                utgifterTotal = 100,
            ),
        beløpsperioder = beløpsperioder,
    )

    private fun vedtaksperiodeGrunnlag(
        fom: LocalDate,
        tom: LocalDate,
    ) = VedtaksperiodeGrunnlag(
        vedtaksperiode =
            VedtaksperiodeBeregning(
                fom = fom,
                tom = tom,
                målgruppe = NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
        aktiviteter = emptyList(),
        antallDager = 4,
    )
}
