package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.tilDelvilkårsett
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.util.UUID

data class OppdaterVilkårsvurderingRestDto(
    val id: UUID,
    val behandlingId: UUID,
    val vurdering: List<VurderingRestDto>,
)

data class VurderingRestDto(
    val regel: RegelId,
    val svar: SvarId?,
    val begrunnelse: String?,
)

fun VurderingRestDto.tilDomene(): Vurdering = Vurdering(
    regelId = this.regel,
    svar = this.svar,
    begrunnelse = this.begrunnelse,
)

fun List<VurderingRestDto>.tilDelvilkårDtoer(stønadstype: Stønadstype): List<DelvilkårDto> {
    val vurderinger = this.map { it.tilDomene() }

    return vurderinger.tilDelvilkårsett(stønadstype).map { it.tilDto() }
}
