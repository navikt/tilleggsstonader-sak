package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
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
fun validerEndringAvAlleredeUtbetaltPeriode(
    nyttBeregningsresultat: BeregningsresultatOffentligTransport,
    reiserForrigeBehandling: List<BeregningsresultatForReise>?,
) {
    val dagensDato = LocalDate.now()

    if (reiserForrigeBehandling.isNullOrEmpty()) return

    for (reise in nyttBeregningsresultat.reiser) {
        val reiseIForrigeBeregningsresultat = reiserForrigeBehandling.filter { it.reiseId == reise.reiseId }
        if (reiseIForrigeBeregningsresultat.isEmpty()) continue
        // Hvis vi kommer hit, vet vi at reisen har blitt endret på i revurderingen

        // Sjekker om det finnes en periode som inneholder dagens dato i revurderingen
        val dagensPeriodeIRevurdering = finnPeriodeMedDato(reise.perioder, dagensDato)

        // Sjekker om det finnes en periode som inneholder dagens dato i førstegangsbehandlingen
        val dagensPeriodeIFørstegangs =
            finnPeriodeMedDato(reiseIForrigeBeregningsresultat.flatMap { it.perioder }, dagensDato)

        // Sjekk om perioden som inneholder dagens dato sin billetttype endrer seg fra enkeltbilletter til månedskort
        val endrerFraEnkeltbilletterTilMånedskort =
            endrerFraEnkeltbilletterTilMånedskort(
                dagensPeriodeIFørstegangs,
                dagensPeriodeIRevurdering,
            )

        brukerfeilHvis(endrerFraEnkeltbilletterTilMånedskort) {
            """
            I den revurderte beregningen vil en allerede utbetalt periode med enkeltbilletter bli endret 
            til en periode med månedskort, som kan være til ugunst for søker. For å hindre dette kan du legge 
            inn en ny reise i stedet for å forlenge den eksisterende.
            """.trimIndent()
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

fun validerReiser(
    vilkår: List<VilkårDagligReise>,
    vedtaksperioder: List<Vedtaksperiode>,
) {
    brukerfeilHvis(vilkår.isEmpty()) {
        "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise"
    }

    brukerfeilHvisIkke(finnesReiseHeleVedtaksperioden(vilkår, vedtaksperioder)) {
        "Kan ikke innvilge for valgte perioder fordi det ikke finnes vilkår for reise for alle vedtaksperioder."
    }
}

private fun finnesReiseHeleVedtaksperioden(
    vilkår: List<VilkårDagligReise>,
    vedtaksperioder: List<Vedtaksperiode>,
): Boolean {
    val sammenslåtteReiser =
        vilkår
            .sorted()
            .mergeSammenhengende { p1, p2 -> p1.overlapperEllerPåfølgesAv(p2) }

    return vedtaksperioder.all { vedtaksperiode ->
        sammenslåtteReiser.any { it.inneholder(vedtaksperiode) }
    }
}
