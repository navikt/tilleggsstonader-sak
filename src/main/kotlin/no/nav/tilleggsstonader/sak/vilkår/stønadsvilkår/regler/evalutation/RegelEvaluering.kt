package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

/**
 * @param vilkårType type vilkår
 * @param vilkår [Vilkårsresultat] for vilkåret
 * @param delvilkår [Vilkårsresultat] for hver hovedregel
 */
data class RegelResultat(
    val vilkårType: VilkårType,
    val vilkår: Vilkårsresultat,
    val delvilkår: Map<RegelId, Vilkårsresultat>,
) {

    fun resultatHovedregel(hovedregel: RegelId) =
        delvilkår[hovedregel] ?: throw Feil("Savner resultat for regelId=$hovedregel vilkårType=$vilkårType")
}

object RegelEvaluering {

    /**
     * @return [RegelResultat] med resultat for vilkåret og delvilkår
     */
    fun utledResultat(vilkårsregel: Vilkårsregel, delvilkårsett: List<DelvilkårDto>): RegelResultat {
        val delvilkårResultat = delvilkårsett.associate { delvilkår ->
            delvilkår.hovedregel() to utledResultatForDelvilkår(vilkårsregel, delvilkår)
        }
        return RegelResultat(
            vilkårType = vilkårsregel.vilkårType,
            vilkår = utledVilkårResultat(delvilkårResultat),
            delvilkår = delvilkårResultat,
        )
    }

    fun utledVilkårResultat(delvilkårResultat: Map<RegelId, Vilkårsresultat>): Vilkårsresultat {
        return when {
            delvilkårResultat.isEmpty() -> Vilkårsresultat.OPPFYLT // TODO vurder denne, skal kanskje kun gjelde målgruppe/aktiitet?
            delvilkårResultat.values.all { it == Vilkårsresultat.OPPFYLT || it == Vilkårsresultat.AUTOMATISK_OPPFYLT } -> Vilkårsresultat.OPPFYLT
            delvilkårResultat.values.all { it == Vilkårsresultat.OPPFYLT || it == Vilkårsresultat.IKKE_OPPFYLT || it == Vilkårsresultat.AUTOMATISK_OPPFYLT } ->
                Vilkårsresultat.IKKE_OPPFYLT
            delvilkårResultat.values.any { it == Vilkårsresultat.SKAL_IKKE_VURDERES } -> Vilkårsresultat.SKAL_IKKE_VURDERES
            delvilkårResultat.values.any { it == Vilkårsresultat.IKKE_TATT_STILLING_TIL } ->
                Vilkårsresultat.IKKE_TATT_STILLING_TIL
            else -> error("Håndterer ikke situasjonen med resultat=${delvilkårResultat.values}")
        }
    }

    /**
     * Dette setter foreløpig resultat, men fortsetter å validere resterende svar slik att man fortsatt har ett gyldig svar
     */
    private fun utledResultatForDelvilkår(
        vilkårsregel: Vilkårsregel,
        delvilkår: DelvilkårDto,
    ): Vilkårsresultat {
        delvilkår.vurderinger.forEach { svar ->
            val regel = vilkårsregel.regel(svar.regelId)
            val svarId = svar.svar ?: return Vilkårsresultat.IKKE_TATT_STILLING_TIL
            val svarMapping = regel.svarMapping(svarId)

            if (RegelValidering.manglerPåkrevdBegrunnelse(svarMapping, svar)) {
                return Vilkårsresultat.IKKE_TATT_STILLING_TIL
            }

            if (svarMapping is SluttSvarRegel) {
                return svarMapping.resultat.vilkårsresultat
            }
        }
        error("Noe gikk galt, skal ikke komme til sluttet her")
    }
}
