package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningDato
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.validering.UtgifterValideringUtil

object BoutgifterUtgifterValideringUtil {
    /**
     * Antakelser:
     * - Det har allerede blitt validert at utgiftene ikke strekker seg lengre tilbake i tid fra søknadsdato enn det som
     * gir rett på stønad (hhv. 3/6 mnd for utgifter overnatting/løpende utgifter).
     *
     */
    fun validerUtgifter(utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>) {
        brukerfeilHvis(detFinnesBådeLøpendeOgMidlertidigeUtgifter(utgifter)) {
            "Foreløpig støtter vi ikke løpende og midlertidige utgifter i samme behandling"
        }
        UtgifterValideringUtil.validerUtgifter(utgifter)
    }

    private fun detFinnesBådeLøpendeOgMidlertidigeUtgifter(utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>): Boolean {
        val finnesLøpendeUtgifter =
            utgifter.keys.any {
                it == TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG || it == TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER
            }
        val finnesUtgifterOvernatting = utgifter.keys.any { it == TypeBoutgift.UTGIFTER_OVERNATTING }
        return finnesLøpendeUtgifter && finnesUtgifterOvernatting
    }
}
