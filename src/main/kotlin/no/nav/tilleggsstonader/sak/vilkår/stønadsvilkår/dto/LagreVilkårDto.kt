package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import java.util.UUID

interface LagreVilkårDto {
    val behandlingId: UUID
    val delvilkårsett: List<DelvilkårDto>
    // TODO: fom/tom og utgifter
}

data class SvarPåVilkårDto(
    val id: UUID,
    override val behandlingId: UUID,
    override val delvilkårsett: List<DelvilkårDto>,
) : LagreVilkårDto

data class OpprettVilkårDto(
    val barnId: UUID,
    override val behandlingId: UUID,
    override val delvilkårsett: List<DelvilkårDto>,
) : LagreVilkårDto
