package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.json

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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.vilkårsreglerPassBarn
import java.time.LocalDateTime
import java.util.UUID

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

fun Vilkår.tilJson() = this.tilDto().tilJson()

fun VilkårDto.tilJson(): VilkårJson {
    val vurderinger: List<VurderingDto> = this.delvilkårsett.flatMap { it.vurderinger }

    return VilkårJson(
        id = this.id,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        vilkårType = this.vilkårType,
        barnId = this.barnId,
        endretAv = this.endretAv,
        endretTid = this.endretTid,
        vurdering = vurderinger.tilJson(),
        opphavsvilkår = this.opphavsvilkår,
    )
}

private fun List<VurderingDto>.tilJson(): VilkårsvurderingJson {
    val vilkårsvurdering = mutableMapOf<RegelId, DelvilkårsvurderingJson>()
    val stønadsregler = vilkårsreglerPassBarn()
    for ((regel, regelSteg) in stønadsregler) {
        val vurderingDto = this.find { it.regelId == regel }

        vilkårsvurdering[regel] = delvilkårsvurderingMapper(regel, vurderingDto, regelSteg.svarMapping)
    }
    return vilkårsvurdering
}

private fun delvilkårsvurderingMapper(
    gjeldendeRegel: RegelId,
    vurdering: VurderingDto?,
    svarMapping: Map<SvarId, SvarRegel>,
): DelvilkårsvurderingJson {
    val svaralternativer = svarMapping.entries.associate {
        it.key to SvaralternativJson(it.value.begrunnelseType)
    }

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
            .map { OverordnetValgJson(it.erAvhengigAvDenneRegelen, it.ogDetteSvaret) }.firstOrNull()

    return DelvilkårsvurderingJson(
        følgerFraOverordnetValg = følgerFraOverordnetValg,
        svar = vurdering?.svar,
        begrunnelse = vurdering?.begrunnelse,
        svaralternativer = svaralternativer,
    )
}

data class DelvilkårsvurderingJson(
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
    val svaralternativer: Map<SvarId, SvaralternativJson>,
    val følgerFraOverordnetValg: OverordnetValgJson? = null,
)

data class OverordnetValgJson(
    val regel: RegelId,
    val svar: SvarId,
)

data class SvaralternativJson(
    val begrunnelsestype: BegrunnelseType,
)
