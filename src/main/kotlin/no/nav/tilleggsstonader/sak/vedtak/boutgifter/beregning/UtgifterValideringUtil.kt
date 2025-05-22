package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift

object UtgifterValideringUtil {
    /**
     * Antakelser:
     * - Det har allerede blitt validert at utgiftene ikke strekker seg lengre tilbake i tid fra søknadsdato enn det som
     * gir rett på stønad (hhv. 3/6 mnd for utgifter overnatting/løpende utgifter).
     *
     */
    fun validerUtgifter(utgifter: BoutgifterPerUtgiftstype) {
        brukerfeilHvis(utgifter.values.flatten().isEmpty()) {
            "Det er ikke lagt inn noen oppfylte utgiftsperioder"
        }
        brukerfeilHvis(detFinnesBådeLøpendeOgMidlertidigeUtgifter(utgifter)) {
            "Foreløpig støtter vi ikke løpende og midlertidige utgifter i samme behandling"
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

    private fun detFinnesBådeLøpendeOgMidlertidigeUtgifter(utgifter: BoutgifterPerUtgiftstype): Boolean {
        val finnesLøpendeUtgifter =
            utgifter.keys.any {
                it == TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG || it == TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER
            }
        val finnesUtgifterOvernatting = utgifter.keys.any { it == TypeBoutgift.UTGIFTER_OVERNATTING }
        return finnesLøpendeUtgifter && finnesUtgifterOvernatting
    }
}
