package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import java.time.LocalDate
import java.util.UUID

sealed interface LagreVilkårDto {
    val behandlingId: UUID
    val delvilkårsett: List<DelvilkårDto>
    val fom: LocalDate?
    val tom: LocalDate?
    val utgift: Int?
}

data class SvarPåVilkårDto(
    val id: UUID,
    override val behandlingId: UUID,
    override val delvilkårsett: List<DelvilkårDto>,
    override val fom: LocalDate?,
    override val tom: LocalDate?,
    override val utgift: Int?,
) : LagreVilkårDto

data class OpprettVilkårDto(
    val vilkårType: VilkårType,
    val barnId: UUID,
    override val behandlingId: UUID,
    override val delvilkårsett: List<DelvilkårDto>,
    override val fom: LocalDate?,
    override val tom: LocalDate?,
    override val utgift: Int?,
) : LagreVilkårDto
