package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat.AUTOMATISK_OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat.IKKE_OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat.IKKE_TATT_STILLING_TIL
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat.OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat.SKAL_IKKE_VURDERES
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel

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
    fun utledResultat(
        vilkårsregel: Vilkårsregel,
        delvilkårsett: List<DelvilkårDto>,
    ): RegelResultat {
        val delvilkårResultat =
            delvilkårsett.associate { delvilkår ->
                delvilkår.hovedregel() to utledResultatForDelvilkår(vilkårsregel, delvilkår)
            }
        return RegelResultat(
            vilkårType = vilkårsregel.vilkårType,
            vilkår = utledVilkårResultat(delvilkårResultat),
            delvilkår = delvilkårResultat,
        )
    }

    fun utledVilkårResultat(delvilkårResultat: Map<RegelId, Vilkårsresultat>): Vilkårsresultat {
        val resultat = delvilkårResultat.values
        return when {
            resultat.all { it == OPPFYLT || it == AUTOMATISK_OPPFYLT } -> OPPFYLT
            resultat.all { it == OPPFYLT || it == IKKE_OPPFYLT || it == AUTOMATISK_OPPFYLT } -> IKKE_OPPFYLT
            resultat.any { it == SKAL_IKKE_VURDERES } -> SKAL_IKKE_VURDERES
            resultat.any { it == IKKE_TATT_STILLING_TIL } -> IKKE_TATT_STILLING_TIL

            else -> error("Håndterer ikke situasjonen med resultat=$resultat")
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
            val svarId = svar.svar ?: return IKKE_TATT_STILLING_TIL
            val svarMapping = regel.svarMapping(svarId)

            if (RegelValidering.manglerPåkrevdBegrunnelse(svarMapping, svar)) {
                return IKKE_TATT_STILLING_TIL
            }

            if (svarMapping is SluttSvarRegel) {
                return svarMapping.resultat.vilkårsresultat
            }
        }
        error("Noe gikk galt, skal ikke komme til sluttet her")
    }
}
