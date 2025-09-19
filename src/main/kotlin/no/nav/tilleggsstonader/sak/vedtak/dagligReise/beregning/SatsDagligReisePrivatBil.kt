package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class SatsDagligReisePrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beløp: BigDecimal,
) : Periode<LocalDate>

val satser: List<SatsDagligReisePrivatBil> =
    listOf(
        SatsDagligReisePrivatBil(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 12, 31),
            beløp = BigDecimal("2.88"),
        ),
        SatsDagligReisePrivatBil(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 12, 31),
            beløp = BigDecimal("2.79"),
        ),
        SatsDagligReisePrivatBil(
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 12, 31),
            beløp = BigDecimal("2.62"),
        ),
    )

fun finnRelevantKilometerSats(periode: Periode<LocalDate>): BigDecimal =
    satser.find { it.inneholder(periode) }?.beløp
        ?: error("Kan ikke finne relevant kilometersats for $periode")
