package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.util.TakeIfUtil.takeIfType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetsdager
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.MålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TomVurdering

/**
 * Temporær mapping då vilkårperiode.vilkårOgFakta fortsatt brukes rundt om i koden
 */
object VilkårFaktaMapper {

    fun Vilkårperiode.mapTilVilkårFakta(): VilkårOgFakta {
        return VilkårOgFakta(
            type = type,
            fom = fom,
            tom = tom,
            begrunnelse = begrunnelse,
            delvilkår = when (faktaOgVurdering) {
                is AktivitetTilsynBarn -> {
                    val vurderinger = faktaOgVurdering.vurderinger
                    when (vurderinger) {
                        is TomVurdering -> DelvilkårAktivitet(
                            lønnet = DelvilkårVilkårperiode.Vurdering(null, ResultatDelvilkårperiode.IKKE_VURDERT),
                        )

                        is LønnetVurdering -> DelvilkårAktivitet(
                            lønnet = vurderinger.lønnet,
                        )

                        else -> error("Finnes ikke")
                    }
                }

                is MålgruppeTilsynBarn -> DelvilkårMålgruppe(
                    medlemskap = faktaOgVurdering.vurderinger.medlemskap,
                    dekketAvAnnetRegelverk = faktaOgVurdering.vurderinger.dekketAvAnnetRegelverk,
                )

                else -> error("Finnes ikke")
            },
            aktivitetsdager = faktaOgVurdering.fakta.takeIfType<FaktaAktivitetsdager>()?.aktivitetsdager,
        )
    }
}
