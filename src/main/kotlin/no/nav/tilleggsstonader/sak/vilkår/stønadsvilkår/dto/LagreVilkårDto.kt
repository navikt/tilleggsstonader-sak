package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import java.time.YearMonth
import java.util.UUID

sealed interface LagreVilkårDto {
    val behandlingId: UUID
    val delvilkårsett: List<DelvilkårDto>
    val fom: YearMonth?
    val tom: YearMonth?
    val beløp: Int?
}

data class SvarPåVilkårDto(
    val id: UUID,
    override val behandlingId: UUID,
    override val delvilkårsett: List<DelvilkårDto>,
    override val fom: YearMonth?,
    override val tom: YearMonth?,
    override val beløp: Int?,
) : LagreVilkårDto

data class OpprettVilkårDto(
    val vilkårType: VilkårType,
    val barnId: UUID,
    override val behandlingId: UUID,
    override val delvilkårsett: List<DelvilkårDto>,
    override val fom: YearMonth?,
    override val tom: YearMonth?,
    override val beløp: Int?,
) : LagreVilkårDto
