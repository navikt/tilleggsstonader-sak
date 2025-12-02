package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OffentligTransportBeregningValidering {
    fun validerRevurdering(
        beregnignsresultat: BeregningsresultatOffentligTransport,
        tidligsteEndring: LocalDate?,
        forrigeVedtak: InnvilgelseEllerOpphørDagligReise,
    ): BeregningsresultatForReise {
        brukerfeilHvis(tidligsteEndring == null) {
            "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen"
        }
        // TO-DO denne må byttes ut til LocalDate.now()
        val idag = LocalDate.of(2025, 2, 10)
        val perioderSomSkalBeregnes = beregnignsresultat.reiser.flatMap { it.perioder }

        // Sjekk om førstegangsbehandlingen hadde en periode som dekker dagens dato
        val førstegangsbehandlingHarDagensDatoiPeriode =
            harPeriodeMedDato(
                forrigeVedtak.beregningsresultat.offentligTransport?.reiser ?: emptyList(),
                idag,
            )

        // Sjekk om revurderingen har en periode som dekker dagens dato
        val revurderingHarDagensDatoIPeriode =
            perioderSomSkalBeregnes.any { periode ->
                idag in periode.grunnlag.fom..periode.grunnlag.tom
            }

        // Finn perioden som gjelder i dag i førstegangsbehandlingen
        val dagensPeriodeIFørstegangs =
            finnPeriodeMedDato(
                forrigeVedtak.beregningsresultat.offentligTransport
                    ?.reiser
                    ?.flatMap { it.perioder }
                    ?: emptyList(),
                idag,
            )

        // Finn perioden som gjelder i dag i revurderingen
        val dagensPeriodeIRevurdering = finnPeriodeMedDato(perioderSomSkalBeregnes, idag)

        // Sjekk om periode-typen endrer seg fra enkeltbilletter → månedskort
        val endrerFraEnkeltbilletterTilMånedskort =
            endrerFraEnkeltbilletterTilMånedskort(
                dagensPeriodeIFørstegangs,
                dagensPeriodeIRevurdering,
            )

        if (førstegangsbehandlingHarDagensDatoiPeriode &&
            revurderingHarDagensDatoIPeriode &&
            endrerFraEnkeltbilletterTilMånedskort
        ) {
            brukerfeil(
                "Kan ikke endre fra enkeltbilletter til månedskort i en periode som allerede er aktiv. " +
                    "Legg inn månedskortet som en egen reise.",
            )
        } else {
            return BeregningsresultatForReise(
                perioder = perioderSomSkalBeregnes,
            )
        }
    }

    private fun harPeriodeMedDato(
        reiser: List<BeregningsresultatForReise>,
        dato: LocalDate,
    ): Boolean = reiser.any { reise -> reise.perioder.any { periode -> dato in periode.grunnlag.fom..periode.grunnlag.tom } }

    private fun finnPeriodeMedDato(
        perioder: List<BeregningsresultatForPeriode>,
        dato: LocalDate,
    ): BeregningsresultatForPeriode? = perioder.firstOrNull { periode -> dato in periode.grunnlag.fom..periode.grunnlag.tom }

    private fun endrerFraEnkeltbilletterTilMånedskort(
        førstegangs: BeregningsresultatForPeriode?,
        revurdering: BeregningsresultatForPeriode?,
    ): Boolean =
        førstegangs != null &&
            revurdering != null &&
            førstegangs.beløp < (revurdering.grunnlag.pris30dagersbillett ?: Int.MAX_VALUE) &&
            revurdering.beløp == revurdering.grunnlag.pris30dagersbillett
}
