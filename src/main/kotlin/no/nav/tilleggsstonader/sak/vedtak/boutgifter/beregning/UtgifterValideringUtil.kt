package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningDato
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift

object UtgifterValideringUtil {
    /**
     * Antakelser:
     * - Det har allerede blitt validert at utgiftene ikke strekker seg lengre tilbake i tid fra søknadsdato enn det som
     * gir rett på stønad (hhv. 3/6 mnd for utgifter overnatting/løpende utgifter).
     *
     */
    fun validerUtgifter(utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>) {
        brukerfeilHvis(erUtgifterOvernattingOgLøpendeUtgifter(utgifter)) {
            "Foreløbig støtter vi ikke faste- og midlertidig utgift i samme behandling"
        }
        brukerfeilHvis(utgifter.values.flatten().isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen utgiftsperioder"
        }
        utgifter.entries.forEach { (type, utgiftsperioderAvGittType) ->
            feilHvis(utgiftsperioderAvGittType.overlapper()) {
                "Utgiftsperioder av type $type overlapper"
            }

            val ikkePositivUtgift = utgiftsperioderAvGittType.firstOrNull { it.utgift < 0 }?.utgift
            feilHvis(ikkePositivUtgift != null) {
                "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
            }
        }
    }

    private fun erUtgifterOvernattingOgLøpendeUtgifter(utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>): Boolean {
        val erLøpendeUtgifter =
            utgifter.keys.any {
                it == TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG || it == TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER
            }
        val erUtgifterOvernatting = utgifter.keys.any { it == TypeBoutgift.UTGIFTER_OVERNATTING }
        return erLøpendeUtgifter && erUtgifterOvernatting
    }
}
