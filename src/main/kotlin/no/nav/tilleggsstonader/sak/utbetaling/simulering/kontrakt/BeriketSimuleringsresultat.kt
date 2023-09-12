package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

import java.math.BigDecimal
import java.time.LocalDate

data class BeriketSimuleringsresultat(
    val detaljer: DetaljertSimuleringResultat,
    val oppsummering: Simuleringsoppsummering,
)

data class DetaljertSimuleringResultat(val simuleringMottaker: List<SimuleringMottaker>)

data class SimuleringMottaker(
    val simulertPostering: List<SimulertPostering>, // perioder
    val mottakerNummer: String? = null,
    val mottakerType: MottakerType,
) {

    override fun toString(): String {
        return (javaClass.simpleName + "< mottakerType=" + mottakerType + ">")
    }
}

data class SimulertPostering(
    val fagOmrådeKode: FagOmrådeKode,
    val erFeilkonto: Boolean? = null, // brukes for å skille manuelle korigeringer og reelle feilutbetalinger
    val fom: LocalDate,
    val tom: LocalDate,
    val betalingType: BetalingType,
    val beløp: BigDecimal,
    val posteringType: PosteringType,
    val forfallsdato: LocalDate,
    val utenInntrekk: Boolean = false,
)
