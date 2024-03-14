package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto

data class VilkårsvurderingDtoGammel(
    val vilkårsett: List<VilkårDtoGammel>,
    val grunnlag: BehandlingFaktaDto,
)
