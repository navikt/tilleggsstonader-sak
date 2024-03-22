package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.rest

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpphavsvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkårsreglerPassBarn
import java.time.LocalDateTime
import java.util.UUID

data class VilkårRestDto(
    val id: UUID,
    val behandlingId: UUID,
    val resultat: Vilkårsresultat,
    val vilkårType: VilkårType,
    val barnId: UUID? = null,
    val endretAv: String,
    val endretTid: LocalDateTime,
    val delvilkårsett: DelvilkårsettRestDto,
    val opphavsvilkår: OpphavsvilkårDto?,
)

private typealias DelvilkårsettRestDto = Map<RegelId, DelvilkårRestDto>

data class DelvilkårRestDto(
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
    val svaralternativer: Map<SvarId, SvaralternativRestDto>,
    val følgerFraOverordnetValg: OverordnetValgRestDto? = null,
)

data class OverordnetValgRestDto(
    val regel: RegelId,
    val svar: SvarId,
)

data class SvaralternativRestDto(
    val begrunnelsestype: BegrunnelseType,
)

fun Vilkår.tilRestDto() = this.tilDto().tilRestDto()

fun VilkårDto.tilRestDto(): VilkårRestDto {
    val delvilkårsett = this.delvilkårsett.flatMap { it.vurderinger }

    return VilkårRestDto(
        id = this.id,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        vilkårType = this.vilkårType,
        barnId = this.barnId,
        endretAv = this.endretAv,
        endretTid = this.endretTid,
        delvilkårsett = delvilkårsett.tilRestDto(),
        opphavsvilkår = this.opphavsvilkår,
    )
}

private fun List<VurderingDto>.tilRestDto(): DelvilkårsettRestDto {
    val stønadsregler = vilkårsreglerPassBarn()
    return stønadsregler.entries.associate { (regel, regelSteg) ->
        val vurderingDto = find { it.regelId == regel }
        regel to DelvilkårRestDto(
            følgerFraOverordnetValg = finnOverordnetValg(regel),
            svar = vurderingDto?.svar,
            begrunnelse = vurderingDto?.begrunnelse,
            svaralternativer = finnSvaralternativer(regelSteg.svarMapping),
        )
    }
}

private fun finnSvaralternativer(svarMapping: Map<SvarId, SvarRegel>): Map<SvarId, SvaralternativRestDto> {
    return svarMapping.entries.associate {
        it.key to SvaralternativRestDto(it.value.begrunnelseType)
    }
}

private fun finnOverordnetValg(gjeldendeRegel: RegelId): OverordnetValgRestDto? {
    data class Regelavhengighet(
        val denneRegelen: RegelId,
        val erAvhengigAvDenneRegelen: RegelId,
        val ogDetteSvaret: SvarId,
    )

    val relaterteOverordnedeValg = vilkårsreglerPassBarn().values.flatMap { regelSteg ->
        regelSteg.svarMapping.map {
            Regelavhengighet(
                denneRegelen = it.value.regelId,
                erAvhengigAvDenneRegelen = regelSteg.regelId,
                ogDetteSvaret = it.key,
            )
        }
    }

    val følgerFraOverordnetValg =
        relaterteOverordnedeValg.filter { it.denneRegelen == gjeldendeRegel }
            .map { OverordnetValgRestDto(it.erAvhengigAvDenneRegelen, it.ogDetteSvaret) }.firstOrNull()

    return følgerFraOverordnetValg
}
