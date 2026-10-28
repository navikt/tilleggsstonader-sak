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
            besøkteRegler: MutableSet<RegelId>,
        ) {
            if (!besøkteRegler.add(regelId)) return
            val regler =
                vilkårsregel
                    .regel(regelId)
                    .svarMapping.values
                    .map(SvarRegel::regelId)
                    .filterNot { it == RegelId.SLUTT_NODE }
            regler.forEach { getRegler(vilkårsregel, it, besøkteRegler) }
        }
        vilkårsregler.forEach { vilkårsregel ->
            val alleRegler = vilkårsregel.regler.keys
            val reglerFraHovedregler = mutableSetOf<RegelId>()
            vilkårsregel.hovedregler.forEach { getRegler(vilkårsregel, it, reglerFraHovedregler) }
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
