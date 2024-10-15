package no.nav.tilleggsstonader.sak.vedtak.læremidler

import java.time.YearMonth
import no.nav.tilleggsstonader.kontrakter.felles.Periode

private val MAX = YearMonth.of(2099, 12)

data class SatsLæremidler(
    override val fom: YearMonth,
    override val tom: YearMonth,
    val beløp: Map<Studienivå, Int>,
) : Periode<YearMonth> {
    init {
        validatePeriode()
    }
}

val satser: List<SatsLæremidler> = listOf(
    SatsLæremidler(
        fom = YearMonth.of(2024, 1),
        tom = MAX,
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 438, Studienivå.HØYERE_UTDANNING to 875),
    ), SatsLæremidler(
        fom = YearMonth.of(2023, 1),
        tom = YearMonth.of(2023, 12),
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 411, Studienivå.HØYERE_UTDANNING to 822),
    )
)

fun finnSats(måned: YearMonth, studienivå: Studienivå): Int {
    val sats = satser.find { måned in it.fom..it.tom } ?: error("Finner ikke satser for $måned")
    return sats.beløp[studienivå] ?: error("Kan ikke håndtere satser for studienivå=$studienivå")
}
