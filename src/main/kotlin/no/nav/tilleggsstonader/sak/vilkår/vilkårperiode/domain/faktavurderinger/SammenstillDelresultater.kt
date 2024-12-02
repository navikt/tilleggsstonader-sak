package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode

fun sammenstillDelresultater(vararg delresultater: ResultatDelvilkårperiode) = when {
    delresultater.contains(ResultatDelvilkårperiode.IKKE_VURDERT) -> {
        ResultatVilkårperiode.IKKE_VURDERT
    }

    delresultater.contains(ResultatDelvilkårperiode.IKKE_OPPFYLT) -> {
        ResultatVilkårperiode.IKKE_OPPFYLT
    }

    delresultater.all { it == ResultatDelvilkårperiode.OPPFYLT } -> {
        ResultatVilkårperiode.OPPFYLT
    }

    else -> {
        error("Ugyldig resultat ($delresultater)")
    }
}