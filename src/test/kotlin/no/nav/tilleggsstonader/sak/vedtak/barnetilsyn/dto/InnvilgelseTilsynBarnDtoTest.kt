package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class InnvilgelseTilsynBarnDtoTest {

    val stønadsperiodeId = UUID.randomUUID()

    @Test
    fun `skal mappe til dto`() {
        val dto = BeregningsresultatTilsynBarn(
            perioder = listOf(
                beregningsresultatForMåned(
                    stønadsperiodeGrunnlag = listOf(
                        stønadsperiodeGrunnlag(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4)),
                        stønadsperiodeGrunnlag(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16)),
                    ),
                ),
            ),
        ).tilDto()

        assertThat(dto.perioder).containsExactlyInAnyOrder(
            BeregningsresultatForMånedDto(
                dagsats = 10.toBigDecimal(),
                grunnlag = BeregningsgrunnlagDto(
                    måned = YearMonth.of(2024, 1),
                    utgifterTotal = 100,
                    antallBarn = 4,
                    stønadsperioderGrunnlag = listOf(
                        StønadsperiodeGrunnlagDto(
                            stønadsperiode = StønadsperiodeDto(
                                id = stønadsperiodeId,
                                målgruppe = MålgruppeType.AAP,
                                aktivitet = AktivitetType.TILTAK,
                                fom = LocalDate.of(2024, 1, 2),
                                tom = LocalDate.of(2024, 1, 4),
                            ),
                        ),
                        StønadsperiodeGrunnlagDto(
                            stønadsperiode = StønadsperiodeDto(
                                id = stønadsperiodeId,
                                målgruppe = MålgruppeType.AAP,
                                aktivitet = AktivitetType.TILTAK,
                                fom = LocalDate.of(2024, 1, 15),
                                tom = LocalDate.of(2024, 1, 16),
                            ),
                        ),
                    ),
                ),
                månedsbeløp = 100,
            ),
        )
    }

    @Test
    fun `gjelder fra og til skal mappes fra min og maks ax stønadsperioder`() {
        val dto = BeregningsresultatTilsynBarn(
            perioder = listOf(
                beregningsresultatForMåned(
                    stønadsperiodeGrunnlag = listOf(
                        stønadsperiodeGrunnlag(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4)),
                        stønadsperiodeGrunnlag(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16)),
                    ),
                ),
                beregningsresultatForMåned(
                    stønadsperiodeGrunnlag = listOf(
                        stønadsperiodeGrunnlag(LocalDate.of(2024, 2, 3), LocalDate.of(2024, 2, 4)),
                    ),
                ),
            ),
        )
            .tilDto()
        assertThat(dto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 2))
        assertThat(dto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 2, 4))
    }

    private fun beregningsresultatForMåned(stønadsperiodeGrunnlag: List<StønadsperiodeGrunnlag>) =
        BeregningsresultatForMåned(
            dagsats = 10.toBigDecimal(),
            månedsbeløp = 100,
            grunnlag = Beregningsgrunnlag(
                måned = YearMonth.of(2024, 1),
                makssats = 1000,
                stønadsperioderGrunnlag = stønadsperiodeGrunnlag,
                utgifter = emptyList(),
                antallBarn = 4,
                utgifterTotal = 100,
            ),
            beløpsperioder = emptyList(),
        )

    private fun stønadsperiodeGrunnlag(
        fom: LocalDate,
        tom: LocalDate,
    ) = StønadsperiodeGrunnlag(
        stønadsperiode = StønadsperiodeDto(
            id = stønadsperiodeId,
            fom = fom,
            tom = tom,
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
        ),
        aktiviteter = emptyList(),
        antallDager = 4,
    )
}
