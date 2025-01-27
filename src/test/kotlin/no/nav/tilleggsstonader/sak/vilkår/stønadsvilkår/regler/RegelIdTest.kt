package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegelIdTest {
    private val vilkårsregler = Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.values

    @Test
    internal fun `det finnes en kobling til alle regler`() {
        fun getRegler(
            vilkårsregel: Vilkårsregel,
            regelId: RegelId,
        ): List<RegelId> {
            val regler =
                vilkårsregel
                    .regel(regelId)
                    .svarMapping.values
                    .map(SvarRegel::regelId)
                    .filterNot { it == RegelId.SLUTT_NODE }
            val childrenRegler = regler.flatMap { getRegler(vilkårsregel, it) }
            return regler + childrenRegler
        }
        vilkårsregler.forEach { vilkårsregel ->
            val alleRegler = vilkårsregel.regler.keys
            val reglerFraHovedregler =
                vilkårsregel.hovedregler +
                    vilkårsregel.hovedregler
                        .flatMap { getRegler(vilkårsregel, it) }
            assertThat(alleRegler).containsExactlyInAnyOrderElementsOf(reglerFraHovedregler)
        }
    }

    @Test
    internal fun `valider att rotregler er definiert i regler for hver vilkår`() {
        vilkårsregler.forEach {
            assertThat(it.regler.keys).containsAll(it.hovedregler)
        }
    }
}
