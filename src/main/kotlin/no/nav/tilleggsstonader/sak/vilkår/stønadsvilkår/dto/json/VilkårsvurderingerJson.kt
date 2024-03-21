package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.json

import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaDto

data class VilkårsvurderingerJson(
    val vilkårsett: List<VilkårJson>,
    val grunnlag: BehandlingFaktaDto,
)