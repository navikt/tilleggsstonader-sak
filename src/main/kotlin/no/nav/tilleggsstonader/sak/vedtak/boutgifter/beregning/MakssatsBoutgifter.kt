package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
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

private val MAX = 31 desember 2099

val bekreftedeSatser =
    listOf(
        MakssatsBoutgifter(
            fom = 1 januar 2026,
            tom = 31 desember 2026,
            beløp = 5062,
        ),
        MakssatsBoutgifter(
            fom = 1 januar 2025,
            tom = 31 desember 2025,
            beløp = 4953,
        ),
        MakssatsBoutgifter(
            fom = 1 januar 2024,
            tom = 31 desember 2024,
            beløp = 4809,
        ),
        MakssatsBoutgifter(
            fom = 1 januar 2023,
            tom = 31 desember 2023,
            beløp = 4519,
        ),
        MakssatsBoutgifter(
            fom = 1 januar 2022,
            tom = 31 desember 2022,
            beløp = 4396,
        ),
        MakssatsBoutgifter(
            fom = 1 januar 2021,
            tom = 31 desember 2021,
            beløp = 4340,
        ),
        MakssatsBoutgifter(
            fom = 1 januar 2020,
            tom = 31 desember 2020,
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
