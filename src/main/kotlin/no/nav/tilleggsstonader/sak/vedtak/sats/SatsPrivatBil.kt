package no.nav.tilleggsstonader.sak.vedtak.sats

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

data class SatsPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beløp: BigDecimal,
    val bekreftet: Boolean = true,
) : Periode<LocalDate>,
    KopierPeriode<SatsPrivatBil> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ) = SatsPrivatBil(
        fom = fom,
        tom = tom,
        beløp = beløp,
        bekreftet = bekreftet,
    )
}

private val MAX = LocalDate.of(2099, 12, 31)

private val bekreftedeSatser: List<SatsPrivatBil> =
    listOf(
        SatsPrivatBil(
            fom = 1 januar 2026,
            tom = 31 desember 2026,
            beløp = BigDecimal("2.94"),
        ),
        SatsPrivatBil(
            fom = 1 januar 2025,
            tom = 31 desember 2025,
            beløp = BigDecimal("2.88"),
        ),
        SatsPrivatBil(
            fom = 1 januar 2024,
            tom = 31 desember 2024,
            beløp = BigDecimal("2.79"),
        ),
        SatsPrivatBil(
            fom = 1 januar 2023,
            tom = 31 desember 2023,
            beløp = BigDecimal("2.62"),
        ),
    )

val satser: List<SatsPrivatBil> =
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
class SatsPrivatBilProvider {
    val alleSatser: List<SatsPrivatBil>
        get() = satser

    fun finnRelevantKilometerSatsForPeriode(periode: Periode<LocalDate>): SatsPrivatBil =
        alleSatser.find { it.inneholder(periode) }
            ?: error("Kan ikke finne relevant kilometersats for $periode")

    fun finnAlleSatserInnenforPeriode(periode: Periode<LocalDate>) = alleSatser.filter { it.overlapper(periode) }

    fun finnSatsForÅr(år: Int) =
        alleSatser.single {
            it.fom.year == år && it.tom.year == år
        }
}
