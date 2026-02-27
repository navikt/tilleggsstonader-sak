package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

data class SatsDagligReisePrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beløp: BigDecimal,
    val bekreftet: Boolean = true,
) : Periode<LocalDate>

private val MAX = LocalDate.of(2099, 12, 31)

private val bekreftedeSatser: List<SatsDagligReisePrivatBil> =
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

private val satser: List<SatsDagligReisePrivatBil> =
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
class SatsDagligReisePrivatBilProvider {
    val alleSatser: List<SatsDagligReisePrivatBil>
        get() = satser

    fun finnRelevantKilometerSatsForPeriode(periode: Periode<LocalDate>): SatsDagligReisePrivatBil =
        alleSatser.find { it.inneholder(periode) }
            ?: error("Kan ikke finne relevant kilometersats for $periode")
}
