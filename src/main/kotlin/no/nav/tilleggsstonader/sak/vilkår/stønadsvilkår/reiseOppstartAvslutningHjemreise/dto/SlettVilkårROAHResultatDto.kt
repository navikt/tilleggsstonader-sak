package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårROAHDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårROAHMapper.mapTilVilkårROAH

data class SlettVilkårRequestDto(
    val kommentar: String? = null,
)

data class SlettVilkårResultatDto(
    val slettetPermanent: Boolean,
    val vilkår: VilkårROAHDto,
)

fun SlettetVilkårResultat.tilROAHDto() =
    SlettVilkårResultatDto(
        slettetPermanent = this.slettetPermanent,
        vilkår = this.vilkår.mapTilVilkårROAH().tilDto(),
    )
