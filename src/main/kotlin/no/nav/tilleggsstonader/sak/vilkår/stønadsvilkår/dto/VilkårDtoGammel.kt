package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.tilJson
import java.time.LocalDateTime
import java.util.UUID

data class VilkårDtoGammel(
    val id: UUID,
    val behandlingId: UUID,
    val resultat: Vilkårsresultat,
    val vilkårType: VilkårType,
    val barnId: UUID? = null,
    val endretAv: String,
    val endretTid: LocalDateTime,
    val delvilkårsett: List<DelvilkårDto> = emptyList(),
    val opphavsvilkår: OpphavsvilkårDto?,
)

data class VilkårJson(
    val id: UUID,
    val behandlingId: UUID,
    val resultat: Vilkårsresultat,
    val vilkårType: VilkårType,
    val barnId: UUID? = null,
    val endretAv: String,
    val endretTid: LocalDateTime,
    val vurdering: VilkårsvurderingJson,
    val opphavsvilkår: OpphavsvilkårDto?,
)

typealias VilkårsvurderingJson = Map<RegelId, DelvilkårsvurderingJson>

data class DelvilkårsvurderingJson(
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
    val svaralternativer: Map<SvarId, SvaralternativJson>,
    val følgerFraOverordnetValg: OverordnetValgJson? = null,
)

data class OppdaterVilkårsvurderingJson(
    val id: UUID,
    val behandlingId: UUID,
    val vurdering: List<VurderingJson>,
)

data class VurderingJson(
    val regel: RegelId,
    val svar: SvarId,
    val begrunnelse: String?,
)

data class OverordnetValgJson(
    val regel: RegelId,
    val svar: SvarId,
)

data class OpphavsvilkårDto(
    val behandlingId: UUID,
    val endretTid: LocalDateTime,
)

data class SvaralternativJson(
    val begrunnelsestype: BegrunnelseType,
)

data class OppdaterVilkårDto(val id: UUID, val behandlingId: UUID)

data class SvarPåVilkårDto(
    val id: UUID,
    val behandlingId: UUID,
    val delvilkårsett: List<DelvilkårDto>,
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
    VilkårDtoGammel(
        id = this.id,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        vilkårType = this.type,
        barnId = this.barnId,
        endretAv = this.sporbar.endret.endretAv,
        endretTid = this.sporbar.endret.endretTid,
        delvilkårsett = this.delvilkårsett
            .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
            .map { it.tilDto() },
        opphavsvilkår = this.opphavsvilkår?.let { OpphavsvilkårDto(it.behandlingId, it.vurderingstidspunkt) },
    )

fun Vilkår.tilJson() = this.tilDto().tilJson()

fun DelvilkårDto.svarTilDomene() = this.vurderinger.map { it.tilDomene() }
fun VurderingDto.tilDomene() = Vurdering(this.regelId, this.svar, this.begrunnelse)
