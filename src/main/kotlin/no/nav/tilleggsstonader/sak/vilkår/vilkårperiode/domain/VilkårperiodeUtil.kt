package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering

object VilkårperiodeUtil {

    inline fun <reified T : FaktaOgVurdering> List<GeneriskVilkårperiode<*>>.ofType(): List<GeneriskVilkårperiode<T>> {
        @Suppress("UNCHECKED_CAST")
        return this.filter { it.faktaOgVurdering is T } as List<GeneriskVilkårperiode<T>>
    }

    inline fun <reified T : FaktaOgVurdering> GeneriskVilkårperiode<*>.takeIfType(): GeneriskVilkårperiode<T>? {
        @Suppress("UNCHECKED_CAST")
        return this.takeIf { it.faktaOgVurdering is T } as GeneriskVilkårperiode<T>?
    }

    inline fun <reified T : FaktaOgVurdering> GeneriskVilkårperiode<*>.withTypeOrThrow(): GeneriskVilkårperiode<T> {
        require(this.faktaOgVurdering is T)
        @Suppress("UNCHECKED_CAST")
        return this as GeneriskVilkårperiode<T>
    }
}
