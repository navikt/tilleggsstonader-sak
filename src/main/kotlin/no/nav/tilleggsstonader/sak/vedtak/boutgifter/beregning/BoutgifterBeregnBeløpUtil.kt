package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import kotlin.math.min

object BoutgifterBeregnBeløpUtil {
    fun beregnBeløp(
        utgifter: Map<Unit, List<UtgiftBeregning>>,
        makssats: Int,
    ): Int {
        val summerteUtgifter = utgifter.values.flatten().sumOf { it.utgift }
        return min(summerteUtgifter, makssats)
    }
}
