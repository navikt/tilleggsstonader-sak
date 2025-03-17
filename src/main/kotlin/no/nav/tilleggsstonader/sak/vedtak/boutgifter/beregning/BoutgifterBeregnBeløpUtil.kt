package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import kotlin.math.min

object BoutgifterBeregnBeløpUtil {
    fun beregnBeløp(
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
        makssats: Int,
    ): Int {
        val summerteUtgifter = utgifter.values.flatten().sumOf { it.utgift }
        return min(summerteUtgifter, makssats)
    }
}
