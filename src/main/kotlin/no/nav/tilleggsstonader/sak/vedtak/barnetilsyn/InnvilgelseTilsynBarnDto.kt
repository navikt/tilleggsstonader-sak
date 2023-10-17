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
    val utgifter: Map<UUID, List<Utgift>>,
)

data class Stønadsperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

data class Utgift(
    override val fom: YearMonth,
    override val tom: YearMonth,
    val utgift: Int,
) : Periode<YearMonth>

/**
 * Hvordan betales dagsats ut fra økonomi? Hvordan skal vi egentlige iverksette perioder som strekker seg fra aug - des?
 * Hvordan virker egentlige dagsats? Sender vi eks 20.08 - 31.08 med 100kr som stønadsbeløp og det betales ut per dag i den perioden? Hva skjer med helg/røde dager?
 */
data class BeregningsresultatTilsynBarnDto(
    val perioder: List<Beregningsresultat>,
)

data class Beregningsresultat(
    override val fom: YearMonth,
    override val tom: YearMonth,
    val makssats: Int,
    val dagsats: Int,
    val grunnlag: Beregningsgrunnlag,
): Periode<YearMonth>, Mergeable<YearMonth, Beregningsresultat> {
    override fun merge(other: Beregningsresultat): Beregningsresultat {
        return this.copy(tom = other.tom)
    }

}

// Map<YearMonth, Beregningsgrunnlag>
// Grunnlag for beregnet periode
// TODO Hvordan burde denne se ut?
data class Beregningsgrunnlag(
    val måned: YearMonth,
    val antallDager: Int,
    val utgifter: List<UtgiftForBarn>
)

data class UtgiftForBarn(
    val barnId: UUID,
    val utgift: Int,
)
