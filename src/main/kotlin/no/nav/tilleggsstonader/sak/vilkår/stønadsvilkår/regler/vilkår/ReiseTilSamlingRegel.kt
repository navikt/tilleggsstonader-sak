package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

class ReiseTilSamlingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.REISE_TIL_SAMLING,
        regler = emptySet(), // TODO("lage regler for reise til samling")
    ) {
    companion object {
        private val REGELER = emptySet<RegelSteg>()
    }
}
