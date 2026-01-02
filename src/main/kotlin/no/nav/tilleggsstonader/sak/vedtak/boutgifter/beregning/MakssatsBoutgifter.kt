package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import org.springframework.stereotype.Component
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

val bekreftedeSatser =
    listOf(
        MakssatsBoutgifter(
            fom = LocalDate.of(2026, 1, 1),
            tom = LocalDate.of(2026, 12, 31),
            beløp = 5062,
        ),
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
        MakssatsBoutgifter(
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 12, 31),
            beløp = 4519,
        ),
        MakssatsBoutgifter(
            fom = LocalDate.of(2022, 1, 1),
            tom = LocalDate.of(2022, 12, 31),
            beløp = 4396,
        ),
        MakssatsBoutgifter(
            fom = LocalDate.of(2021, 1, 1),
            tom = LocalDate.of(2021, 12, 31),
            beløp = 4340,
        ),
        MakssatsBoutgifter(
            fom = LocalDate.of(2020, 1, 1),
            tom = LocalDate.of(2020, 12, 31),
            beløp = 4193,
        ),
    )

val satser: List<MakssatsBoutgifter> =
    listOf(
        bekreftedeSatser.max().let {
            it.copy(
                fom = it.tom.plusDays(1),
                tom = MAX,
                bekreftet = false,
            )
        },
    ) + bekreftedeSatser

@Component
class SatsBoutgifterProvider {
    val alleSatser: List<MakssatsBoutgifter>
        get() = satser
}

@Component
class SatsBoutgifterService(
    private val satsBoutgifterProvider: SatsBoutgifterProvider,
) {
    fun alleSatser() = satsBoutgifterProvider.alleSatser

    fun finnMakssats(dato: LocalDate): MakssatsBoutgifter = satsBoutgifterProvider.alleSatser.finnMakssats(dato)
}

fun List<MakssatsBoutgifter>.finnMakssats(dato: LocalDate): MakssatsBoutgifter =
    find { makssats ->
        dato >= makssats.fom && dato <= makssats.tom
    } ?: error("Finner ikke makssats for $dato")
