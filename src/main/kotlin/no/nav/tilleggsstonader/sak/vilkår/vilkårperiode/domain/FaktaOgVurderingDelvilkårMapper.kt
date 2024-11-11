package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FellesMålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.IngenMålgruppeTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.LønnetVurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OmstillingsstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.OvergangssstønadTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SykepengerTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.TomVurdering

/**
 * TODO fjerne før merge til main
 * Temporær mapping då vilkårperiode.vilkårOgFakta fortsatt brukes rundt om i koden
 */
object FaktaOgVurderingDelvilkårMapper {

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    fun FaktaOgVurdering.tilDelvilkår() = when (this) {
        is AktivitetTilsynBarn -> {
            val vurderinger = this.vurderinger
            when (vurderinger) {
                is TomVurdering -> DelvilkårAktivitet(
                    lønnet = DelvilkårVilkårperiode.Vurdering(null, ResultatDelvilkårperiode.IKKE_VURDERT),
                )

                is LønnetVurdering -> DelvilkårAktivitet(
                    lønnet = vurderinger.lønnet,
                )

                else -> error("Har ikke mapper $vurderinger")
            }
        }

        is IngenMålgruppeTilsynBarn,
        is SykepengerTilsynBarn,
        -> DelvilkårMålgruppe(
            medlemskap = DelvilkårVilkårperiode.Vurdering(null, ResultatDelvilkårperiode.IKKE_AKTUELT),
            dekketAvAnnetRegelverk = DelvilkårVilkårperiode.Vurdering(null, ResultatDelvilkårperiode.IKKE_AKTUELT),
        )
        is OmstillingsstønadTilsynBarn -> DelvilkårMålgruppe(
            medlemskap = this.vurderinger.medlemskap,
            dekketAvAnnetRegelverk = DelvilkårVilkårperiode.Vurdering(null, ResultatDelvilkårperiode.IKKE_AKTUELT),
        )
        is OvergangssstønadTilsynBarn -> DelvilkårMålgruppe(
            medlemskap = this.vurderinger.medlemskap,
            dekketAvAnnetRegelverk = DelvilkårVilkårperiode.Vurdering(null, ResultatDelvilkårperiode.IKKE_AKTUELT),
        )
        is FellesMålgruppeTilsynBarn -> DelvilkårMålgruppe(
            medlemskap = this.vurderinger.medlemskap,
            dekketAvAnnetRegelverk = this.vurderinger.dekketAvAnnetRegelverk,
        )
        else -> error("Har ikke mappet $this")
    }
}
