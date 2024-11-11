@file:Suppress("ktlint:standard:filename") // Kan fjernes når flere vurderinger er lagt til

package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode

sealed interface LønnetVurdering : Vurderinger {
    val lønnet: DelvilkårVilkårperiode.Vurdering
}
