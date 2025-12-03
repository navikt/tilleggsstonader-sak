package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import java.time.LocalDate

fun validerRevurdering(
    beregnignsresultat: BeregningsresultatOffentligTransport,
    tidligsteEndring: LocalDate?,
    forrigeVedtak: InnvilgelseEllerOpphørDagligReise,
) {
    brukerfeilHvis(tidligsteEndring == null) {
        "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen"
    }
    val dagensDato = LocalDate.now()
    val perioderSomSkalBeregnes = beregnignsresultat.reiser.flatMap { it.perioder }

    // Finn perioden som gjelder i dag i førstegangsbehandlingen
    val dagensPeriodeIFørstegangs =
        forrigeVedtak.beregningsresultat.offentligTransport
            ?.reiser
            ?.flatMap { it.perioder }
            ?.firstOrNull { periode -> periode.grunnlag.inneholder(dagensDato) }

    // Finn perioden som gjelder i dag i revurderingen
    val dagensPeriodeIRevurdering = finnPeriodeMedDato(perioderSomSkalBeregnes, dagensDato)

    // Sjekk om periode-typen endrer seg fra enkeltbilletter → månedskort
    val endrerFraEnkeltbilletterTilMånedskort =
        endrerFraEnkeltbilletterTilMånedskort(
            dagensPeriodeIFørstegangs,
            dagensPeriodeIRevurdering,
        )

    if (dagensPeriodeIFørstegangs != null &&
        dagensPeriodeIRevurdering != null &&
        endrerFraEnkeltbilletterTilMånedskort
    ) {
        brukerfeil(
            "Kan ikke endre fra enkeltbilletter til månedskort i en periode som allerede er aktiv. " +
                "Legg inn månedskortet som en egen reise.",
        )
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
