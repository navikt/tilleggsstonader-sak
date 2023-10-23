package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * @param utgifter map utgifter per [no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn]
 */
data class InnvilgelseTilsynBarnDto(
    val stønadsperioder: List<Stønadsperiode>,
    val utgifter: Map<UUID, List<Utgift>>,
)

data class Stønadsperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

data class Utgift(
    override val fom: YearMonth,
    override val tom: YearMonth,
    val utgift: Int,
) : Periode<YearMonth> {
    init {
        validatePeriode()
    }
}

data class BeregningsresultatTilsynBarnDto(
    val perioder: List<Beregningsresultat>,
)

data class Beregningsresultat(
    val dagsats: BigDecimal,
    val månedsbeløp: Int,
    val grunnlag: Beregningsgrunnlag,
)

/**
 * @param makssats er snitt per måned
 */
data class Beregningsgrunnlag(
    val måned: YearMonth,
    val makssats: Int,
    val stønadsperioder: List<Stønadsperiode>,
    val utgifter: List<UtgiftBarn>,
    val antallDagerTotal: Int,
    val utgifterTotal: Int,
    val antallBarn: Int,
)

data class UtgiftBarn(
    val barnId: UUID,
    val utgift: Int,
)
