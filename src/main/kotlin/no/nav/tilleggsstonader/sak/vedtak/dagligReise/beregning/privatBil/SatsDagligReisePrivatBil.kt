package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
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
            fom = 1 januar 2026,
            tom = 31 desember 2026,
            beløp = BigDecimal("2.94"),
        ),
        SatsDagligReisePrivatBil(
            fom = 1 januar 2025,
            tom = 31 desember 2025,
            beløp = BigDecimal("2.88"),
        ),
        SatsDagligReisePrivatBil(
            fom = 1 januar 2024,
            tom = 31 desember 2024,
            beløp = BigDecimal("2.79"),
        ),
        SatsDagligReisePrivatBil(
            fom = 1 januar 2023,
            tom = 31 desember 2023,
            beløp = BigDecimal("2.62"),
        ),
    )

/**
 * Tar kun hensyn til fra datoen slik at vi klarer å håndtere uker som strekker seg
 * over til et nytt år. Det vil gi maks 4 dager med feil sats.
 */
fun finnRelevantKilometerSats(periode: Periode<LocalDate>): BigDecimal =
    satser.find { it.inneholder(periode.fom) }?.beløp
        ?: error("Kan ikke finne relevant kilometersats for $periode")
