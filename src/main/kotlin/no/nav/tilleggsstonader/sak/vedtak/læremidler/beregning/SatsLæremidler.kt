package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import java.time.LocalDate

data class SatsLæremidler(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beløp: Map<Studienivå, Int>,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

private val MAX = LocalDate.of(2099, 12, 31)

val satser: List<SatsLæremidler> = listOf(
    SatsLæremidler(
        fom = LocalDate.of(2024, 1, 1),
        tom = MAX,
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 438, Studienivå.HØYERE_UTDANNING to 875),
    ),
    SatsLæremidler(
        fom = LocalDate.of(2023, 1, 1),
        tom = LocalDate.of(2023, 12, 31),
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 411, Studienivå.HØYERE_UTDANNING to 822),
    ),
)

fun finnSatsForStudienivå(periode: Periode<LocalDate>, studienivå: Studienivå): Int {
    val sats = satser.find { it.inneholder(periode) } ?: error("Finner ikke satser for $periode")
    return sats.beløp[studienivå] ?: error("Kan ikke håndtere satser for studienivå=$studienivå")
}
