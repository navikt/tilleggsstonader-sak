package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår

data class SlettetVilkårResultat(
    val slettetPermanent: Boolean,
    val vilkår: Vilkår,
)
