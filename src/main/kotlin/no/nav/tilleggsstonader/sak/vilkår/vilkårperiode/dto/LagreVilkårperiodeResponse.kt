package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

data class LagreVilkårperiodeResponse(
    val periode: VilkårperiodeDto? = null,
    val stønadsperiodeStatus: Stønadsperiodestatus,
    val stønadsperiodeFeil: String? = null,
)

enum class Stønadsperiodestatus { OK, FEIL }
