package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.YearMonth

data class MakssatsTilsynBarn(
    override val fom: YearMonth,
    override val tom: YearMonth,
    val beløp: Map<Int, Int>,
) : Periode<YearMonth> {
    init {
        validatePeriode()
    }
}

private val MAX = YearMonth.of(2099, 12)

/**
 * I https://lovdata.no/forskrift/2015-07-02-867/§10 refereres det til stønaden som gis EF-barnetilsyn
 * https://lovdata.no/nav/rundskriv/v7-15-00
 *
 * Selv om vi har en dagsats, og det vises makimalt beløp per dag har vi valgt å bruke maks per måned,
 * då vi tolker loven som at det er det vi skal gjøre. Det medfører ingen direkte ulemper for søker
 * https://www.nav.no/tilleggsstonader
 */
val satser: List<MakssatsTilsynBarn> =
    listOf(
        MakssatsTilsynBarn(
            fom = YearMonth.of(2024, 1),
            tom = MAX,
            beløp = mapOf(1 to 4650, 2 to 6066, 3 to 6875),
        ),
        MakssatsTilsynBarn(
            fom = YearMonth.of(2023, 7),
            tom = YearMonth.of(2023, 12),
            beløp = mapOf(1 to 4480, 2 to 5844, 3 to 6623),
        ),
        MakssatsTilsynBarn(
            fom = YearMonth.of(2023, 1),
            tom = YearMonth.of(2023, 6),
            beløp = mapOf(1 to 4369, 2 to 5700, 3 to 6460),
        ),
    )

fun finnMakssats(måned: YearMonth, antallBarn: Int): Int {
    val sats = satser.find { måned in it.fom..it.tom }
        ?: error("Finner ikke satser for $måned")
    return when {
        antallBarn == 1 -> sats.beløp[1]
        antallBarn == 2 -> sats.beløp[2]
        antallBarn > 2 -> sats.beløp[3]
        else -> null
    } ?: error("Kan ikke håndtere satser for antallBarn=$antallBarn")
}
