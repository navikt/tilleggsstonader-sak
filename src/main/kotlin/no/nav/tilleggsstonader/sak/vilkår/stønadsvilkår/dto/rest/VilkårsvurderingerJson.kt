package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto

data class VilkårsvurderingerJson(
    val vilkårsett: List<VilkårRestDto>,
    val grunnlag: BehandlingFaktaDto,
)
