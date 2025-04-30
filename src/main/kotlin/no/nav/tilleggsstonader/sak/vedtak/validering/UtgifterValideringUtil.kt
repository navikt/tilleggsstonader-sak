package no.nav.tilleggsstonader.sak.vedtak.validering

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningType
import java.time.temporal.Temporal

object UtgifterValideringUtil {
    fun <T> validerUtgifter(utgifter: Map<*, List<UtgiftBeregningType<T>>>) where T : Comparable<T>, T : Temporal {
        brukerfeilHvis(utgifter.values.flatten().isEmpty()) {
            "Det er ikke lagt inn noen oppfylte utgiftsperioder"
        }

        utgifter.entries.forEach { (type, utgiftsperioderAvGittType) ->
            feilHvis(utgiftsperioderAvGittType.overlapper()) {
                "Utgiftsperioder overlapper"
            }

            val ikkePositivUtgift = utgiftsperioderAvGittType.firstOrNull { it.utgift < 0 }?.utgift
            feilHvis(ikkePositivUtgift != null) {
                "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
            }
        }
    }
}
