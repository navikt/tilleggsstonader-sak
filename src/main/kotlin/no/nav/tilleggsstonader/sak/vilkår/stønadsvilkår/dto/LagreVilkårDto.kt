package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import java.util.UUID

sealed interface LagreVilkårDto {
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
    val vilkårType: VilkårType,
    val barnId: UUID,
    override val behandlingId: UUID,
    override val delvilkårsett: List<DelvilkårDto>,
) : LagreVilkårDto
