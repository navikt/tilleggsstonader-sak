package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import java.util.UUID

data class VilkårsoppsummeringDto(
    val aktivitet: Boolean,
    val målgruppe: Boolean,
    val stønadsperiode: Boolean,
    val passBarn: List<BarnOppsummering>,
)

data class BarnOppsummering(
    val barnId: UUID,
    val ident: String,
    val navn: String,
    val alder: Int?,
    val alderNårStønadsperiodeBegynner: Int?,
    val oppfyllerAlleVilkår: Boolean,
)
