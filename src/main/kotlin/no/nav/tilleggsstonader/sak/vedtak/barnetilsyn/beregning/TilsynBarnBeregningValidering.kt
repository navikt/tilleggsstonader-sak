package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.brukPerioderFraOgMedRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.brukPerioderFraOgMedRevurderFraMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.erOverlappMellomStønadsperioderOgUtgifter
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import java.time.LocalDate

object TilsynBarnBeregningValidering {
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
        validerStønadsperioderOverlapperUtgifterEtterRevurderFra(stønadsperioder, utgifter, revurderFra)
    }

    private fun validerStønadsperioderOverlapperUtgifterEtterRevurderFra(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
        revurderFra: LocalDate?,
    ) {
        brukerfeilHvisIkke(
            erOverlappMellomStønadsperioderOgUtgifter(
                stønadsperioder = stønadsperioder.brukPerioderFraOgMedRevurderFra(revurderFra),
                utgifter = utgifter.brukPerioderFraOgMedRevurderFraMåned(revurderFra),
            ),
        ) {
            if (revurderFra != null) {
                "Kan ikke innvilge når det ikke finnes noen overlapp mellom målgruppe, aktivitet og utgifter etter revurder fra dato"
            } else {
                "Kan ikke innvilge når det ikke finnes noen overlapp mellom målgruppe, aktivitet og utgifter"
            }
        }
    }

    private fun validerStønadsperioder(stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>) {
        brukerfeilHvis(stønadsperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen overlappende målgruppe og aktivitet"
        }
    }

    private fun validerAktiviteter(aktiviteter: List<Aktivitet>) {
        feilHvis(aktiviteter.isEmpty()) {
            "Aktiviteter mangler"
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
