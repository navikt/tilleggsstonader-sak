package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering

object VilkårperiodeUtil {

    inline fun <reified T : FaktaOgVurdering> List<GeneriskVilkårperiode<*>>.ofType(): List<GeneriskVilkårperiode<T>> {
        @Suppress("UNCHECKED_CAST")
        return this.filter { it.faktaOgVurdering is T } as List<GeneriskVilkårperiode<T>>
    }
}
