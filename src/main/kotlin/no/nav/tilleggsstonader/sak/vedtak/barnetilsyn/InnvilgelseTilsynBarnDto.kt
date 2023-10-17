package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Periode
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
    val makssats: Int,
    val dagsats: Float,
    val grunnlag: Beregningsgrunnlag,
)

// TODO kanskje ta med hvor mye av utgiftene som blir dekket? / dagsats innan avkortning
data class Beregningsgrunnlag(
    val måned: YearMonth,
    val stønadsperioder: List<Stønadsperiode>,
    val utgifter: List<UtgiftForBarn>,
    val antallDagerTotal: Int,
    val utgifterTotal: Int,
)

data class UtgiftForBarn(
    val barnId: UUID,
    val utgift: Int,
)
