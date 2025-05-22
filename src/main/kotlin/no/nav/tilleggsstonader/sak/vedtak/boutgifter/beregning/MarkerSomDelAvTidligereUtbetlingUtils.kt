package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import java.time.LocalDate

object MarkerSomDelAvTidligereUtbetlingUtils {
    /**
     * @param dagensDato Brukes kun for testing. I koden skal default verdi `LocalDate.now()` brukes.
     */
    fun List<BeregningsresultatForLøpendeMåned>.markerSomDelAvTidligereUtbetaling(
        perioderFraForrigeVedtak: List<BeregningsresultatForLøpendeMåned>? = null,
        dagensDato: LocalDate = LocalDate.now(),
    ) = map { periode ->
        if (periode.skalMarkereSomDelAvTidligereUtbetaling(perioderFraForrigeVedtak, dagensDato)) {
            periode.markerSomDelAvTidligereUtbetaling()
        } else {
            periode
        }
    }

    private fun BeregningsresultatForLøpendeMåned.skalMarkereSomDelAvTidligereUtbetaling(
        perioderFraForrigeVedtak: List<BeregningsresultatForLøpendeMåned>?,
        dagensDato: LocalDate,
    ) = (perioderFraForrigeVedtak.orEmpty().noenOverlapper(this) || perioderFraForrigeVedtak == null) &&
        this.harUtgiftFørDagensDato(dagensDato)

    private fun List<BeregningsresultatForLøpendeMåned>.noenOverlapper(periode: BeregningsresultatForLøpendeMåned): Boolean =
        this.any { it.overlapper(periode) }

    private fun BeregningsresultatForLøpendeMåned.harUtgiftFørDagensDato(dagensDato: LocalDate): Boolean =
        this.grunnlag.utgifter.values
            .flatten()
            .any { utgift -> utgift.fom < dagensDato }
}
