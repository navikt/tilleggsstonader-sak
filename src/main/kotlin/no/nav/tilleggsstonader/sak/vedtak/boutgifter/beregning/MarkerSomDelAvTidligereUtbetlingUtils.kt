package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned

object MarkerSomDelAvTidligereUtbetlingUtils {
    fun List<BeregningsresultatForLøpendeMåned>.markerSomDelAvTidligereUtbetaling(
        perioderFraForrigeVedtak: List<BeregningsresultatForLøpendeMåned>? = null,
    ) = map { periode ->
        if (periode.skalMarkereSomDelAvTidligereUtbetaling(perioderFraForrigeVedtak)) {
            periode.markerSomDelAvTidligereUtbetaling()
        } else {
            periode
        }
    }

    private fun BeregningsresultatForLøpendeMåned.skalMarkereSomDelAvTidligereUtbetaling(
        perioderFraForrigeVedtak: List<BeregningsresultatForLøpendeMåned>?,
    ) = (perioderFraForrigeVedtak.orEmpty().noenOverlapper(this) || perioderFraForrigeVedtak == null) &&
        this.harUtgiftFørDagensDato()

    private fun List<BeregningsresultatForLøpendeMåned>.noenOverlapper(periode: BeregningsresultatForLøpendeMåned): Boolean =
        this.any { it.overlapper(periode) }
}
