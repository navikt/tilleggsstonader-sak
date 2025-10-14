package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import org.springframework.stereotype.Component
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

    fun finnSatsForStudienivå(studienivå: Studienivå): Int = beløp[studienivå] ?: error("Finner ikke studienivå=$studienivå for sats=$this")
}

private val MAX = LocalDate.of(2099, 12, 31)

val bekreftedeSatser =
    listOf(
        SatsLæremidler(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 12, 31),
            beløp = mapOf(Studienivå.VIDEREGÅENDE to 451, Studienivå.HØYERE_UTDANNING to 901),
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
        SatsLæremidler(
            fom = LocalDate.of(2022, 1, 1),
            tom = LocalDate.of(2022, 12, 31),
            beløp = mapOf(Studienivå.VIDEREGÅENDE to 400, Studienivå.HØYERE_UTDANNING to 800),
        ),
        SatsLæremidler(
            fom = LocalDate.of(2021, 1, 1),
            tom = LocalDate.of(2021, 12, 31),
            beløp = mapOf(Studienivå.VIDEREGÅENDE to 395, Studienivå.HØYERE_UTDANNING to 760),
        ),
        SatsLæremidler(
            fom = LocalDate.of(2020, 1, 1),
            tom = LocalDate.of(2020, 12, 31),
            beløp = mapOf(Studienivå.VIDEREGÅENDE to 382, Studienivå.HØYERE_UTDANNING to 763),
        ),
    )

@Component
class SatsLæremidlerProvider {
    val satser: List<SatsLæremidler>
        get() =
            listOf(
                bekreftedeSatser.first().let {
                    it.copy(
                        fom = it.tom.plusDays(1),
                        tom = MAX,
                        bekreftet = false,
                    )
                },
            ) + bekreftedeSatser
}

@Component
class SatsLæremidlerService(
    private val satsLæremidlerProvider: SatsLæremidlerProvider,
) {
    fun alleSatser() = satsLæremidlerProvider.satser

    fun finnSatsForPeriode(periode: Periode<LocalDate>): SatsLæremidler =
        satsLæremidlerProvider.satser.find {
            it.inneholder(periode)
        } ?: error("Finner ikke satser for $periode")
}
