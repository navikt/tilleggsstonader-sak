package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.time.LocalDate
import java.time.LocalDateTime

data class VilkårDto(
    val id: VilkårId,
    val behandlingId: BehandlingId,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val vilkårType: VilkårType,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val utgift: Int?,
    val erNullvedtak: Boolean,
    val barnId: BarnId? = null,
    val endretAv: String,
    val endretTid: LocalDateTime,
    val delvilkårsett: List<DelvilkårDto> = emptyList(),
    val opphavsvilkår: OpphavsvilkårDto?,
)

data class OpphavsvilkårDto(
    val behandlingId: BehandlingId,
    val endretTid: LocalDateTime,
)

data class OppdaterVilkårDto(
    val id: VilkårId,
    val behandlingId: BehandlingId,
)

data class GjenbrukVilkårDto(
    val behandlingId: BehandlingId,
    val kopierbehandlingId: BehandlingId,
)

data class DelvilkårDto(
    val resultat: Vilkårsresultat,
    val vurderinger: List<VurderingDto>,
) {
    /**
     * @return regelId for første svaret som er hovedregeln på delvilkåret
     */
    fun hovedregel() = this.vurderinger.first().regelId
}

data class VurderingDto(
    val regelId: RegelId,
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
)

fun Vurdering.tilDto() = VurderingDto(this.regelId, this.svar, this.begrunnelse)

fun Delvilkår.tilDto() = DelvilkårDto(this.resultat, this.vurderinger.map { it.tilDto() })

fun Vilkår.tilDto() =
    VilkårDto(
        id = this.id,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        status = this.status,
        vilkårType = this.type,
        fom = this.fom,
        tom = this.tom,
        utgift = this.utgift,
        erNullvedtak = this.erNullvedtak,
        barnId = this.barnId,
        endretAv = this.sporbar.endret.endretAv,
        endretTid = this.sporbar.endret.endretTid,
        delvilkårsett =
            this.delvilkårsett
                .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
                .map { it.tilDto() },
        opphavsvilkår = this.opphavsvilkår?.let { OpphavsvilkårDto(it.behandlingId, it.vurderingstidspunkt) },
    )

fun DelvilkårDto.svarTilDomene() = this.vurderinger.map { it.tilDomene() }

fun VurderingDto.tilDomene() = Vurdering(this.regelId, this.svar, this.begrunnelse)
