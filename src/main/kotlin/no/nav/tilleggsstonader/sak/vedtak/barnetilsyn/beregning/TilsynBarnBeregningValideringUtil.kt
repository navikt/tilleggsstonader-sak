package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode

object TilsynBarnBeregningValideringUtil {
    fun validerPerioderForInnvilgelse(
        vedtaksperioder: List<Vedtaksperiode>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
        typeVedtak: TypeVedtak,
    ) {
        if (typeVedtak == TypeVedtak.OPPHØR) {
            return
        }
        validerVedtaksperioder(vedtaksperioder)
        validerUtgifter(utgifter)
    }

    private fun validerVedtaksperioder(vedtaksperioder: List<Vedtaksperiode>) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen vedtaksperioder"
        }
    }

    private fun validerUtgifter(utgifter: Map<BarnId, List<UtgiftBeregning>>) {
        brukerfeilHvis(utgifter.values.flatten().isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen utgiftsperioder"
        }
        utgifter.entries.forEach { (_, utgifterForBarn) ->
            feilHvis(utgifterForBarn.overlapper()) {
                "Utgiftsperioder overlapper"
            }

            val ikkePositivUtgift = utgifterForBarn.firstOrNull { it.utgift < 0 }?.utgift
            feilHvis(ikkePositivUtgift != null) {
                "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
            }
        }
    }
}
