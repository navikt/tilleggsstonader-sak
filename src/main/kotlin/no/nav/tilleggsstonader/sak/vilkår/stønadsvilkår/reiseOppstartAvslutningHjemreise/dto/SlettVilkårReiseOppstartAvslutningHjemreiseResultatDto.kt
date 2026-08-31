package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårReiseOppstartAvslutningHjemreise

data class SlettVilkårRequestDto(
    val kommentar: String? = null,
)

data class SlettVilkårResultatDto(
    val slettetPermanent: Boolean,
    val vilkår: VilkårReiseOppstartAvslutningHjemreiseDto,
)

fun SlettetVilkårResultat.tilReiseOppstartAvslutningHjemreiseDto() =
    SlettVilkårResultatDto(
        slettetPermanent = this.slettetPermanent,
        vilkår = this.vilkår.mapTilVilkårReiseOppstartAvslutningHjemreise().tilDto(),
    )
