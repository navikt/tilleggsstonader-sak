package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import java.time.LocalDate

object MarkerSomDelAvTidligereUtbetlingUtils {
    /**
     * @param dagensDato Brukes kun for testing. I koden skal default verdi `LocalDate.now()` brukes.
     */

    fun List<BeregningsresultatForPeriode>.markerSomDelAvTidligereUtbetaling(
        perioderFraForrigeVedtak: List<BeregningsresultatForPeriode>? = null,
        dagensDato: LocalDate = LocalDate.now(),
    ) = map { periode ->
        if (periode.skalMarkereSomDelAvTidligereUtbetaling(perioderFraForrigeVedtak, dagensDato)) {
            periode.markerSomDelAvTidligereUtbetaling()
        } else {
            periode
        }
    }

    private fun BeregningsresultatForPeriode.skalMarkereSomDelAvTidligereUtbetaling(
        perioderFraForrigeVedtak: List<BeregningsresultatForPeriode>?,
        dagensDato: LocalDate,
    ) = (
        perioderFraForrigeVedtak.orEmpty().noenOverlapper(
            this.grunnlag,
        ) ||
            perioderFraForrigeVedtak == null
    ) &&
        this.harUtgiftFørDagensDato(dagensDato)

    private fun List<BeregningsresultatForPeriode>.noenOverlapper(periode: BeregningsgrunnlagOffentligTransport): Boolean =
        this.any { it.grunnlag.overlapper(periode) }

    private fun BeregningsresultatForPeriode.harUtgiftFørDagensDato(dagensDato: LocalDate): Boolean = this.grunnlag.fom < dagensDato
}
