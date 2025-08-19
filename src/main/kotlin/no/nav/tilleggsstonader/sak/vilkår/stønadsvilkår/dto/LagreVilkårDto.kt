package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.OffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import java.time.LocalDate

sealed interface LagreVilkårDto {
    val behandlingId: BehandlingId
    val delvilkårsett: List<DelvilkårDto>
    val fom: LocalDate?
    val tom: LocalDate?
    val utgift: Int?
    val erFremtidigUtgift: Boolean?
    val offentligTransport: OffentligTransport?
}

data class SvarPåVilkårDto(
    val id: VilkårId,
    override val behandlingId: BehandlingId,
    override val delvilkårsett: List<DelvilkårDto>,
    override val fom: LocalDate?,
    override val tom: LocalDate?,
    override val utgift: Int?,
    override val erFremtidigUtgift: Boolean?,
    override val offentligTransport: OffentligTransport?,
) : LagreVilkårDto

data class OpprettVilkårDto(
    val vilkårType: VilkårType,
    val barnId: BarnId? = null,
    override val behandlingId: BehandlingId,
    override val delvilkårsett: List<DelvilkårDto>,
    override val fom: LocalDate?,
    override val tom: LocalDate?,
    override val utgift: Int?,
    override val erFremtidigUtgift: Boolean?,
    override val offentligTransport: OffentligTransport?,
) : LagreVilkårDto
