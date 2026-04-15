package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
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
                                    vedtaksperiodeGrunnlag(2 januar 2024, 4 januar 2024),
                                ),
                            beløpsperioder = listOf(Beløpsperiode(1 januar 2024, 20, NEDSATT_ARBEIDSEVNE)),
                        ),
                    ),
            ).tilDto(Beregningsplan(Beregningsomfang.ALLE_PERIODER))

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
                                    vedtaksperiodeGrunnlag(2 januar 2024, 4 januar 2024),
                                    vedtaksperiodeGrunnlag(15 januar 2024, 16 januar 2024),
                                ),
                        ),
                        beregningsresultatForMåned(
                            vedtaksperiodeGrunnlag =
                                listOf(
                                    vedtaksperiodeGrunnlag(3 februar 2024, 4 februar 2024),
                                ),
                        ),
                    ),
            ).tilDto(Beregningsplan(Beregningsomfang.ALLE_PERIODER))

        assertThat(dto.gjelderFraOgMed).isEqualTo(2 januar 2024)
        assertThat(dto.gjelderTilOgMed).isEqualTo(4 februar 2024)
    }

    @Test
    fun `skal ikke filtrere perioder ved gjenbruk av forrige resultat`() {
        val dto =
            BeregningsresultatTilsynBarn(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            januar2024 = YearMonth.of(2024, 1),
                            vedtaksperiodeGrunnlag =
                                listOf(
                                    vedtaksperiodeGrunnlag(2 januar 2024, 4 januar 2024),
                                ),
                        ),
                        beregningsresultatForMåned(
                            januar2024 = YearMonth.of(2024, 2),
                            vedtaksperiodeGrunnlag =
                                listOf(
                                    vedtaksperiodeGrunnlag(3 februar 2024, 4 februar 2024),
                                ),
                        ),
                    ),
            ).tilDto(Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT))

        assertThat(dto.perioder.map { it.grunnlag.måned }).containsExactly(YearMonth.of(2024, 1), YearMonth.of(2024, 2))
        assertThat(dto.gjelderFraOgMed).isEqualTo(2 januar 2024)
        assertThat(dto.gjelderTilOgMed).isEqualTo(4 februar 2024)
        assertThat(dto.tidligsteEndring).isNull()
    }

    @Nested
    inner class TidligsteEndringMånedsbeløp {
        /*
         * skal mappe beløp fra perioder som gjelder fra og med tidligste endring då det ikke er ønskelig å vise
         * beløp som allerede er innvilget
         */
        @Test
        fun `skal mappe beløp fra hele måneden når tidligste endring er satt`() {
            val tidligsteEndring = 17 januar 2024
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
                ).tilDto(Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = tidligsteEndring))

            assertThat(dto.perioder.single().månedsbeløp).isEqualTo(60)
        }
    }

    @Nested
    inner class TidligsteEndringGjelderFraOgTil {
        val tidligsteEndring = 17 januar 2024

        @Test
        fun `periode som overlapper skal bruke tidligsteEndring som startdato`() {
            val periode = vedtaksperiodeGrunnlag(fom = 2 januar 2024, tom = 18 januar 2024)
            val dto = resultatMedEnVedtaksperiode(periode).tilDto(Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = tidligsteEndring))

            assertThat(dto.gjelderFraOgMed).isEqualTo(tidligsteEndring)
            assertThat(dto.gjelderTilOgMed).isEqualTo(18 januar 2024)
        }

        @Test
        fun `periode som begynner før tidligsteEndring skal ikke brukes til gjelderFra eller gjelderTil`() {
            val periode = vedtaksperiodeGrunnlag(fom = 2 januar 2024, tom = 16 januar 2024)
            val dto = resultatMedEnVedtaksperiode(periode).tilDto(Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = tidligsteEndring))

            assertThat(dto.gjelderFraOgMed).isNull()
            assertThat(dto.gjelderTilOgMed).isNull()
        }

        @Test
        fun `periode som begynner fra og med tidligsteEndring brukes til gjelderFra og gjelderTil`() {
            val periode = vedtaksperiodeGrunnlag(fom = 17 januar 2024, tom = 19 januar 2024)
            val dto = resultatMedEnVedtaksperiode(periode).tilDto(Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = tidligsteEndring))

            assertThat(dto.gjelderFraOgMed).isEqualTo(17 januar 2024)
            assertThat(dto.gjelderTilOgMed).isEqualTo(19 januar 2024)
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
        januar2024: YearMonth = YearMonth.of(2024, 1),
        vedtaksperiodeGrunnlag: List<VedtaksperiodeGrunnlag> = emptyList(),
        beløpsperioder: List<Beløpsperiode> = emptyList(),
    ) = BeregningsresultatForMåned(
        dagsats = 10.toBigDecimal(),
        månedsbeløp = 100,
        grunnlag =
            Beregningsgrunnlag(
                måned = januar2024,
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
