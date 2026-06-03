package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingDtoMapper.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingMapper.mapTilVilkårReiseTilSamling

data class SlettVilkårRequestDto(
    val kommentar: String? = null,
)

data class SlettVilkårResultatDto(
    val slettetPermanent: Boolean,
    val vilkår: VilkårReiseTilSamlingDto,
)

fun SlettetVilkårResultat.tilDagligreiseDto() =
    SlettVilkårResultatDto(
        slettetPermanent = this.slettetPermanent,
        vilkår = this.vilkår.mapTilVilkårReiseTilSamling().tilDto(),
    )
