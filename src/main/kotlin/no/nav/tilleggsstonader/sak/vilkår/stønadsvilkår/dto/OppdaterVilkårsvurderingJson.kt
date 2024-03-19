package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.hovedreglerPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.vilkårsreglerPassBarn
import java.util.UUID

data class OppdaterVilkårsvurderingJson(
    val id: UUID,
    val behandlingId: UUID,
    val vurdering: List<VurderingJson>,
)

data class VurderingJson(
    val regel: RegelId,
    val svar: SvarId?,
    val begrunnelse: String?,
)

fun List<VurderingJson>.tilDelvilkårDtoer(): List<DelvilkårDto> {
    val resultat = mutableListOf<DelvilkårDto>()

    val hovedregler = hovedreglerPassBarn()
    val stønadsregler = vilkårsreglerPassBarn()

    for (regelId in hovedregler) {
        val relaterteOppdateringer = this.find { it.regel == regelId }!!

        val svar = relaterteOppdateringer.svar!! // alle hovedregler skal ha et svar.
        val begrunnelse = relaterteOppdateringer.begrunnelse

        val skalHaBegrunnelse =
            stønadsregler[regelId]?.svarMapping?.get(svar)?.begrunnelseType != BegrunnelseType.UTEN

        val oppdaterteVurderinger = Vurdering(
            regelId = regelId,
            svar = svar,
            begrunnelse = if (skalHaBegrunnelse) begrunnelse else null,
        )

        val oppfølgingsregel = stønadsregler[regelId]!!.svarMapping[svar]!!.regelId

        if (oppfølgingsregel == RegelId.SLUTT_NODE) {
            resultat.add(Delvilkår(vurderinger = listOf(oppdaterteVurderinger)).tilDto())

            continue
        } else {
            // We got ourselves a oppfølgingsregel over here
            val oppfølgingssvar = this.find { it.regel == oppfølgingsregel }!!
            val oppfølgingsvurdering = Vurdering(
                regelId = oppfølgingsregel,
                svar = oppfølgingssvar.svar,
                begrunnelse = oppfølgingssvar.begrunnelse,
            )

            resultat.add(Delvilkår(vurderinger = listOf(oppdaterteVurderinger, oppfølgingsvurdering)).tilDto())
        }
    }

    return resultat
}
