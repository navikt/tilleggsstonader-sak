package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BeregningsresultatLæremidler(
    val perioder: List<BeregningsresultatForMåned>,
)

data class BeregningsresultatForMåned(
    val beløp: BigDecimal,
    val grunnlag: Beregningsgrunnlag,
)

data class Beregningsgrunnlag(
    val måned: YearMonth,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
)

data class BeregningPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val studienivå: Studienivå,
    val studieprosent: Int,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

enum class Studienivå {
    VIDEREGÅENDE,
    HØYERE_UTDANNING,
}
