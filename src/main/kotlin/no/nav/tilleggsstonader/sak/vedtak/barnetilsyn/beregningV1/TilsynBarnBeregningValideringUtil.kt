package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.validerAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.validerOverlappendePeriodeOgUtgiftEtterRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.validerUtgifter
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import java.time.LocalDate

object TilsynBarnBeregningValideringUtil {
    fun validerPerioderForInnvilgelse(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<Aktivitet>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
        typeVedtak: TypeVedtak,
        revurderFra: LocalDate?,
    ) {
        if (typeVedtak == TypeVedtak.OPPHØR) {
            return
        }
        validerStønadsperioder(stønadsperioder)
        validerAktiviteter(aktiviteter)
        validerUtgifter(utgifter)
        validerOverlappendePeriodeOgUtgiftEtterRevurderFra(stønadsperioder, utgifter, revurderFra)
    }

    private fun validerStønadsperioder(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>) {
        brukerfeilHvis(stønadsperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen overlappende målgruppe og aktivitet"
        }
    }
}
