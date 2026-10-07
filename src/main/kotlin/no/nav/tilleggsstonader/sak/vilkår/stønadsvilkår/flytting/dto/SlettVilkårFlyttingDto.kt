package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.SlettetVilkårResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.mapTilVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.VilkårFlyttingMapper.tilVilkårFlyttingDto

data class SlettVilkårFlyttingRequestDto(
    val kommentar: String? = null,
)

data class SlettVilkårFlyttingResultatDto(
    val slettetPermanent: Boolean,
    val vilkår: VilkårFlyttingDto,
)

fun SlettetVilkårResultat.tilVilkårFlyttingDto() =
    SlettVilkårFlyttingResultatDto(
        slettetPermanent = slettetPermanent,
        vilkår = vilkår.mapTilVilkårFlytting().tilVilkårFlyttingDto(),
    )
