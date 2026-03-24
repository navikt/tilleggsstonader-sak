package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode

object UtgifterValideringUtil {
    /**
     * Antakelser:
     * - Det har allerede blitt validert at utgiftene ikke strekker seg lengre tilbake i tid fra søknadsdato enn det som
     * gir rett på stønad (hhv. 3/6 mnd for utgifter overnatting/løpende utgifter).
     *
     */
    fun validerUtgifter(
        utgifter: BoutgifterPerUtgiftstype,
        vedtakstype: TypeVedtak,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        // Tillat opphør av hele saken
        if (vedtakstype == TypeVedtak.OPPHØR && utgifter.values.flatten().isEmpty() && vedtaksperioder.isEmpty()) return

        brukerfeilHvis(utgifter.values.flatten().isEmpty()) {
            "Det er ikke lagt inn noen oppfylte utgiftsperioder"
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
}
