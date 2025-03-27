package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

/**
 * Hvert år endres vanligvis makssatsen for boutgifter. Ny sats gjelder fra og med 1 januar.
 * Når vi innvilget fra 1aug-31mai så deles det opp i 2 utbetalingsperioder, 1aug-31des og 1jan-31mai.
 * Den første perioden har bekreftet sats og utbetales direkte.
 * Den andre perioden har ikke bekreftet sats og utbetales når ny makssats er fastsatt.
 */
data class MakssatsBoutgifter(
    override val fom: LocalDate,
    override val tom: LocalDate,
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
        MakssatsBoutgifter(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 12, 31),
            beløp = 4953,
        ),
        MakssatsBoutgifter(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 12, 31),
            beløp = 4809,
        ),
    )

private val satser: List<MakssatsBoutgifter> =
    listOf(
        bekreftedeSatser.max().let {
            it.copy(
                fom = it.tom.plusDays(1),
                tom = MAX,
                bekreftet = false,
            )
        },
    ) + bekreftedeSatser

fun finnMakssatsForPeriode(dato: LocalDate): MakssatsBoutgifter =
    satser.find {
        dato >= it.fom && dato <= it.tom
    } ?: error("Finner ikke satser for $dato")
