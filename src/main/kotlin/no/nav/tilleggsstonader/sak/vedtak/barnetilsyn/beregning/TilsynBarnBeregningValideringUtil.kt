package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.brukPerioderFraOgMedRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.brukPerioderFraOgMedRevurderFraMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate

object TilsynBarnBeregningValideringUtil {
    fun validerPerioderForInnvilgelse(
        vedtaksperioder: List<Vedtaksperiode>,
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

    private fun <P> validerOverlappendePeriodeOgUtgiftEtterRevurderFra(
        perioder: List<P>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
        revurderFra: LocalDate?,
    ) where P : Periode<LocalDate>, P : KopierPeriode<P> {
        brukerfeilHvisIkke(
            erOverlappMellomPerioderOgUtgifter(
                perioder = perioder.brukPerioderFraOgMedRevurderFra(revurderFra),
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

    fun <P : Periode<LocalDate>> erOverlappMellomPerioderOgUtgifter(
        perioder: List<P>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ): Boolean {
        val utgiftPerioder =
            utgifter.values.flatMap {
                it.map { Datoperiode(fom = it.fom.atDay(1), tom = it.tom.atEndOfMonth()) }
            }
        return utgiftPerioder.any { utgifterPeriode ->
            perioder.any { vedtaksperiode ->
                vedtaksperiode.overlapper(utgifterPeriode)
            }
        }
    }

    private fun validerVedtaksperioder(vedtaksperioder: List<Vedtaksperiode>) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen vedtaksperioder"
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
