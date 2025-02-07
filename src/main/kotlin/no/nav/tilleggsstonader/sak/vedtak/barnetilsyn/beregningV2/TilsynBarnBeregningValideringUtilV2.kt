package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.validerAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.validerOverlappendePeriodeOgUtgiftEtterRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.validerUtgifter
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.time.LocalDate

object TilsynBarnBeregningValideringUtilV2 {
    fun validerPerioderForInnvilgelse(
        vedtaksperioder: List<VedtaksperiodeDto>,
        aktiviteter: List<Aktivitet>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
        typeVedtak: TypeVedtak,
        revurderFra: LocalDate?,
    ) {
        if (typeVedtak == TypeVedtak.OPPHØR) {
            return
        }
        validerVedtaksperioder(vedtaksperioder)
        validerAktiviteter(aktiviteter)
        validerUtgifter(utgifter)
        validerOverlappendePeriodeOgUtgiftEtterRevurderFra(vedtaksperioder, utgifter, revurderFra)
    }

    private fun validerVedtaksperioder(vedtaksperioder: List<VedtaksperiodeDto>) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen vedtaksperioder"
        }
    }
}
