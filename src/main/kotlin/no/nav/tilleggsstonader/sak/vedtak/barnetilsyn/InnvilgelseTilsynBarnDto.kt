package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * @param utgifter map utgifter per [no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn]
 */
data class InnvilgelseTilsynBarnDto(
    val behandlingId: UUID,
    val stønadsperioder: List<Stønadsperiode>,
    val utgifter: Map<UUID, List<Utgift>>
)

data class Stønadsperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

data class Utgift(
    val utgift: Int,
    override val fom: YearMonth
) : FomPeriode<YearMonth>

/**
 * Hvordan betales dagsats ut fra økonomi? Hvordan skal vi egentlige iverksette perioder som strekker seg fra aug - des?
 * Hvordan virker egentlige dagsats? Sender vi eks 20.08 - 31.08 med 100kr som stønadsbeløp og det betales ut per dag i den perioden? Hva skjer med helg/røde dager?
 */
data class BeregningsresultatTilsynBarnDto(
    val perioder: List<Beregningsresultat>
)

data class Beregningsresultat(
    val fom: YearMonth,
    val tom: YearMonth,
    val makssats: Int,
    val dagsats: Int,
    val grunnlag: Beregningsgrunnlag
)

// Map<YearMonth, Beregningsgrunnlag>
// Grunnlag for beregnet periode
// TODO Hvordan burde denne se ut?
data class Beregningsgrunnlag(
    val totalutgift: Int,
    val antallDager: Int,
    val barn: Map<UUID, Int>,

    val perioder: List<Stønadsperiode>
)
