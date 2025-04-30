package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.validering.UtgifterValideringUtil.validerUtgifter

object TilsynBarnBeregningValideringUtil {
    fun validerPerioderForInnvilgelse(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        utgifter: Map<BarnId, List<UtgiftBeregningMåned>>,
        typeVedtak: TypeVedtak,
    ) {
        if (typeVedtak == TypeVedtak.OPPHØR) {
            return
        }
        validerVedtaksperioder(vedtaksperioder)
        validerUtgifter(utgifter)
    }

    private fun validerVedtaksperioder(vedtaksperioder: List<VedtaksperiodeBeregning>) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen vedtaksperioder"
        }
    }
}
