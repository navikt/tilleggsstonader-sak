package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

data class SlettVilkårRequestDto(
    val kommentar: String? = null,
)

data class SlettVilkårResultatDto(
    val slettetPermanent: Boolean,
    val vilkår: VilkårDagligReiseDto,
)
