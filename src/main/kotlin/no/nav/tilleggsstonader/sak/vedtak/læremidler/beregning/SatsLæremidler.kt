package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import java.time.YearMonth

data class SatsLæremidler(
    override val fom: YearMonth,
    override val tom: YearMonth,
    val beløp: Map<Studienivå, Int>,
) : Periode<YearMonth> {
    init {
        validatePeriode()
    }
}

private val MAX = YearMonth.of(2099, 12)

val satser: List<SatsLæremidler> = listOf(
    SatsLæremidler(
        fom = YearMonth.of(2024, 1),
        tom = MAX,
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 438, Studienivå.HØYERE_UTDANNING to 875),
    ),
    SatsLæremidler(
        fom = YearMonth.of(2023, 1),
        tom = YearMonth.of(2023, 12),
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 411, Studienivå.HØYERE_UTDANNING to 822),
    ),
)

fun finnSatsForStudienivå(måned: YearMonth, studienivå: Studienivå): Int {
    val sats = satser.find { måned in it.fom..it.tom } ?: error("Finner ikke satser for $måned")
    return sats.beløp[studienivå] ?: error("Kan ikke håndtere satser for studienivå=$studienivå")
}
