package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import java.time.LocalDate

/**
 * Hvert år endres vanligvis satsen for læremidler. Ny sats gjelder fra og med 1 januar.
 * Når vi innvilget fra 1aug-31mai så deles det opp i 2 utbetalingsperioder, 1aug-31des og 1jan-31mai.
 * Den første perioden har bekreftet sats og utbetales direkte.
 * Den andre perioden har ikke bekreftet sats og utbetales når ny sats er fastsatt.
 */
data class SatsLæremidler(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beløp: Map<Studienivå, Int>,
    val bekreftet: Boolean = true,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

private val MAX = LocalDate.of(2099, 12, 31)

val satser: List<SatsLæremidler> = listOf(
    SatsLæremidler(
        fom = LocalDate.of(2025, 1, 1),
        tom = MAX,
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 438, Studienivå.HØYERE_UTDANNING to 875),
        bekreftet = false,
    ),
    SatsLæremidler(
        fom = LocalDate.of(2024, 1, 1),
        tom = LocalDate.of(2024, 12, 31),
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 438, Studienivå.HØYERE_UTDANNING to 875),
    ),
    SatsLæremidler(
        fom = LocalDate.of(2023, 1, 1),
        tom = LocalDate.of(2023, 12, 31),
        beløp = mapOf(Studienivå.VIDEREGÅENDE to 411, Studienivå.HØYERE_UTDANNING to 822),
    ),
)

fun finnSatsForPeriode(periode: Periode<LocalDate>): SatsLæremidler {
    return satser.find { it.inneholder(periode) } ?: error("Finner ikke satser for $periode")
}

fun finnSatsForStudienivå(sats: SatsLæremidler, studienivå: Studienivå): Int {
    return sats.beløp[studienivå] ?: error("Finner ikke studienivå=$studienivå for sats=$sats")
}
