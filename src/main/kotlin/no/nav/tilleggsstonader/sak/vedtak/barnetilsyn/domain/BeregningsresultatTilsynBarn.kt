package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BeregningsresultatTilsynBarn(
    val perioder: List<BeregningsresultatForMåned>,
)

data class BeregningsresultatForMåned(
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
