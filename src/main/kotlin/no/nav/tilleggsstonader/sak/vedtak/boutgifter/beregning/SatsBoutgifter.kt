package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

/**
 * Hvert år endres vanligvis satsen for boutgifter. Ny sats gjelder fra og med 1 januar.
 * Når vi innvilget fra 1aug-31mai så deles det opp i 2 utbetalingsperioder, 1aug-31des og 1jan-31mai.
 * Den første perioden har bekreftet sats og utbetales direkte.
 * Den andre perioden har ikke bekreftet sats og utbetales når ny sats er fastsatt.
 */
data class SatsBoutgifter(
    override val fom: LocalDate,
    override val tom: LocalDate,
//    val beløp: Map<Studienivå, Int>,
    val beløp: Int,
    val bekreftet: Boolean = true,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

private val MAX = LocalDate.of(2099, 12, 31)

private val bekreftedeSatser =
    listOf(
        SatsBoutgifter(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 12, 31),
//            beløp = mapOf(Studienivå.VIDEREGÅENDE to 451, Studienivå.HØYERE_UTDANNING to 901),
            beløp = TODO("Gjør uavhengig av studienivå"),
        ),
        SatsBoutgifter(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 12, 31),
//            beløp = mapOf(Studienivå.VIDEREGÅENDE to 438, Studienivå.HØYERE_UTDANNING to 875),
            beløp = TODO("Gjør uavhengig av studienivå"),
        ),
        SatsBoutgifter(
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 12, 31),
//            beløp = mapOf(Studienivå.VIDEREGÅENDE to 411, Studienivå.HØYERE_UTDANNING to 822),
            beløp = TODO("Gjør uavhengig av studienivå"),
        ),
    )

val satser: List<SatsBoutgifter> =
    listOf(
        bekreftedeSatser.first().let {
            it.copy(
                fom = it.tom.plusDays(1),
                tom = MAX,
                bekreftet = false,
            )
        },
    ) + bekreftedeSatser

fun finnSatsForPeriode(periode: Periode<LocalDate>): SatsBoutgifter =
    satser.find {
        it.inneholder(periode)
    } ?: error("Finner ikke satser for $periode")

fun finnSatsForStudienivå(
//    sats: SatsBoutgifter,
//    studienivå: Studienivå,
// ): Int = sats.beløp[studienivå] ?: error("Finner ikke studienivå=$studienivå for sats=$sats")
): Int = TODO("Studienivå er ikke relevant for boutgifter")
