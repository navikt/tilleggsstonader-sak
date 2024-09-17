package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * @param utgifter map utgifter per [no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn]
 */
data class InnvilgelseTilsynBarnDto(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) : VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)

data class InnvilgelseTilsynBarnRequest(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) {
    fun tilDto() = InnvilgelseTilsynBarnDto(beregningsresultat)
}

data class Utgift(
    val fom: YearMonth,
    val tom: YearMonth,
    val utgift: Int,
)

data class BeregningsresultatTilsynBarnDto(
    val perioder: List<Beregningsresultat>,
)

data class Beregningsresultat(
    val dagsats: BigDecimal,
    val månedsbeløp: Int,
    val grunnlag: Beregningsgrunnlag,
    val beløpsperioder: List<Beløpsperiode>,
)

/**
 * @param dato tilsvarer fom datoen på en stønadsperiode
 * og er den datoen hele beløpet samlet iversettes på
 */
data class Beløpsperiode(
    val dato: LocalDate,
    val beløp: Int,
    val målgruppe: MålgruppeType,
)

/**
 * @param makssats er snitt per måned
 */
data class Beregningsgrunnlag(
    val måned: YearMonth,
    val makssats: Int,
    val stønadsperioderGrunnlag: List<StønadsperiodeGrunnlag>,
    val utgifter: List<UtgiftBarn>,
    val utgifterTotal: Int,
    val antallBarn: Int,
)

data class StønadsperiodeGrunnlag(
    val stønadsperiode: StønadsperiodeDto,
    val aktiviteter: List<Aktivitet>,
    val antallDager: Int,
)

data class UtgiftBarn(
    val barnId: BarnId,
    val utgift: Int,
)
