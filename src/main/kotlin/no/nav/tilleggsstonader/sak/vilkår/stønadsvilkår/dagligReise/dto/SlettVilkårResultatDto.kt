package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise

data class SlettVilkårRequestDto(
    val kommentar: String? = null,
)

data class SlettVilkårResultatDto(
    val slettetPermanent: Boolean,
    val vilkår: VilkårDagligReiseDto,
)

fun SlettetVilkårResultat.tilDagligreiseDto() =
    SlettVilkårResultatDto(
        slettetPermanent = this.slettetPermanent,
        vilkår = this.vilkår.mapTilVilkårDagligReise().tilDto(),
    )
