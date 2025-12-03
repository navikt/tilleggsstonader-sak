package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import java.time.LocalDate

/**
 * Validerer at en revurdering av offentlig transport ikke endrer en periode som inneholder dagens dato (er tidligere utbetalt) som enkeltbilletter
 * til månedskort, da dette kan være til ugunst for søker. Valideringen resulterer i en brukerfeil som veileder saksbehandler må legge inn en ny reise i stedet for å
 * forlenge en eksisterende periode.
 *
 * For hver reise i det nye beregningsresultatet:
 *  - Sjekker vi om det finnes en tilsvarende reise i forrige beregningsresultat (basert på reiseId).
 *  - Hvis ja, sjekker om det finnes en periode for dagens dato i både revurderingen og førstegangsbehandlingen.
 *  - Dersom endringen på perioden endrer billetttype fra enkeltbilletter til månedskort, kastes en veiledende brukerfeil.
 */
fun validerRevurdering(
    beregningsresultat: BeregningsresultatOffentligTransport,
    tidligsteEndring: LocalDate?,
    forrigeIverksatteVedtak: InnvilgelseEllerOpphørDagligReise,
) {
    brukerfeilHvis(tidligsteEndring == null) {
        "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen"
    }
    val dagensDato = LocalDate.now()

    val nyttBeregningsresultat = beregningsresultat.reiser
    val forrigeBeregningsresultat = forrigeIverksatteVedtak.beregningsresultat.offentligTransport?.reiser ?: return

    for (reise in nyttBeregningsresultat) {
        val overlappendeReise = forrigeBeregningsresultat.filter { it.reiseId == reise.reiseId }
        if (overlappendeReise.isEmpty()) continue
        // Hvis vi kommer hit, vet vi at reisen har blitt endret på i revurderingen

        // Sjekker om det finnes en periode som inneholder dagens dato i revurderingen
        val dagensPeriodeIRevurdering = finnPeriodeMedDato(reise.perioder, dagensDato)

        // Sjekker om det finnes en periode som inneholder dagens dato i førstegangsbehandlingen
        val dagensPeriodeIFørstegangs = finnPeriodeMedDato(overlappendeReise.flatMap { it.perioder }, dagensDato)

        // Sjekk om perioden som inneholder dagens dato sin billetttype endrer seg fra enkeltbilletter til månedskort
        val endrerFraEnkeltbilletterTilMånedskort =
            endrerFraEnkeltbilletterTilMånedskort(
                dagensPeriodeIFørstegangs,
                dagensPeriodeIRevurdering,
            )

        if (endrerFraEnkeltbilletterTilMånedskort) {
            brukerfeil(
                """
                I den revurderte beregningen vil en allerede utbetalt periode med enkeltbilletter bli endret 
                til en periode med månedskort, som kan være til ugunst for søker. For å hindre dette kan du legge 
                inn en ny reise i stedet for å forlenge den eksisterende.
                """.trimIndent(),
            )
        }
    }
}

private fun finnPeriodeMedDato(
    perioder: List<BeregningsresultatForPeriode>,
    dato: LocalDate,
): BeregningsresultatForPeriode? = perioder.firstOrNull { periode -> periode.grunnlag.inneholder(dato) }

private fun endrerFraEnkeltbilletterTilMånedskort(
    førstegangs: BeregningsresultatForPeriode?,
    revurdering: BeregningsresultatForPeriode?,
): Boolean =
    førstegangs != null &&
        revurdering != null &&
        førstegangs.beløp < (revurdering.grunnlag.pris30dagersbillett ?: Int.MAX_VALUE) &&
        revurdering.beløp == revurdering.grunnlag.pris30dagersbillett
