package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class InnvilgelseTilsynBarnDtoKtTest {
    @Test
    fun `skal mappe til dto`() {
        val dto =
            BeregningsresultatTilsynBarn(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            stønadsperiodeGrunnlag =
                                listOf(
                                    stønadsperiodeGrunnlag(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4)),
                                ),
                            beløpsperioder = listOf(Beløpsperiode(LocalDate.of(2024, 1, 1), 20, MålgruppeType.AAP)),
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
    fun `gjelder fra og til skal mappes fra min og maks ax stønadsperioder`() {
        val dto =
            BeregningsresultatTilsynBarn(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            stønadsperiodeGrunnlag =
                                listOf(
                                    stønadsperiodeGrunnlag(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4)),
                                    stønadsperiodeGrunnlag(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16)),
                                ),
                        ),
                        beregningsresultatForMåned(
                            stønadsperiodeGrunnlag =
                                listOf(
                                    stønadsperiodeGrunnlag(LocalDate.of(2024, 2, 3), LocalDate.of(2024, 2, 4)),
                                ),
                        ),
                    ),
            ).tilDto(null)

        assertThat(dto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 2))
        assertThat(dto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 2, 4))
    }

    @Nested
    inner class RevurderFraMånedsbeløp {
        /*
         * skal mappe beløp fra perioder som gjelder fra og med revurderingsdatoet då det ikke er ønskelig å vise
         * beløp som allerede er innvilget
         */
        @Test
        fun `skal mappe beløp fra perioder som gjelder fra og med revurderingsdatoet`() {
            val revurderFra = LocalDate.of(2024, 1, 17)
            val dto =
                BeregningsresultatTilsynBarn(
                    perioder =
                        listOf(
                            beregningsresultatForMåned(
                                beløpsperioder =
                                    listOf(
                                        Beløpsperiode(revurderFra.minusDays(1), 10, MålgruppeType.AAP),
                                        Beløpsperiode(revurderFra, 20, MålgruppeType.AAP),
                                        Beløpsperiode(revurderFra.plusDays(1), 30, MålgruppeType.AAP),
                                    ),
                            ),
                        ),
                ).tilDto(revurderFra)

            assertThat(dto.perioder.single().månedsbeløp).isEqualTo(50)
        }
    }

    @Nested
    inner class RevurderFraGjelderFraOgTil {
        val revurderFra = LocalDate.of(2024, 1, 17)

        @Test
        fun `periode som overlapper skal bruke revurderFra som startdato`() {
            val periode = stønadsperiodeGrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 18))
            val dto = resultatMedEnStønadsperiode(periode).tilDto(revurderFra)

            assertThat(dto.gjelderFraOgMed).isEqualTo(revurderFra)
            assertThat(dto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 18))
        }

        @Test
        fun `periode som begynner før revurderFra skal ikke brukes til gjelderFra eller gjelderTil`() {
            val periode = stønadsperiodeGrunnlag(fom = LocalDate.of(2024, 1, 2), tom = LocalDate.of(2024, 1, 16))
            val dto = resultatMedEnStønadsperiode(periode).tilDto(revurderFra)

            assertThat(dto.gjelderFraOgMed).isNull()
            assertThat(dto.gjelderTilOgMed).isNull()
        }

        @Test
        fun `periode som begynner fra og med revurderFra brukes til gjelderFra og gjelderTil`() {
            val periode = stønadsperiodeGrunnlag(fom = LocalDate.of(2024, 1, 17), tom = LocalDate.of(2024, 1, 19))
            val dto = resultatMedEnStønadsperiode(periode).tilDto(revurderFra)

            assertThat(dto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 17))
            assertThat(dto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 19))
        }

        private fun resultatMedEnStønadsperiode(stønadsperiodeGrunnlag: StønadsperiodeGrunnlag) =
            BeregningsresultatTilsynBarn(
                perioder =
                    listOf(
                        beregningsresultatForMåned(stønadsperiodeGrunnlag = listOf(stønadsperiodeGrunnlag)),
                    ),
            )
    }

    private fun beregningsresultatForMåned(
        stønadsperiodeGrunnlag: List<StønadsperiodeGrunnlag> = emptyList(),
        beløpsperioder: List<Beløpsperiode> = emptyList(),
    ) = BeregningsresultatForMåned(
        dagsats = 10.toBigDecimal(),
        månedsbeløp = 100,
        grunnlag =
            Beregningsgrunnlag(
                måned = YearMonth.of(2024, 1),
                makssats = 1000,
                stønadsperioderGrunnlag = stønadsperiodeGrunnlag,
                utgifter = emptyList(),
                antallBarn = 4,
                utgifterTotal = 100,
            ),
        beløpsperioder = beløpsperioder,
    )

    private fun stønadsperiodeGrunnlag(
        fom: LocalDate,
        tom: LocalDate,
    ) = StønadsperiodeGrunnlag(
        stønadsperiode =
            VedtaksperiodeBeregning(
                id = UUID.randomUUID(),
                fom = fom,
                tom = tom,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            ),
        aktiviteter = emptyList(),
        antallDager = 4,
    )
}
