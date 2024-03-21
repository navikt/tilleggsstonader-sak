package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto

data class VilkårsvurderingDto(
    val vilkårsett: List<VilkårDto>,
    val grunnlag: BehandlingFaktaDto,
)
