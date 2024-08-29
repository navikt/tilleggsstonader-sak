package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.utledResultatForVilkårSomGjelderFlereBarn

object VilkårsresultatUtil {

    fun utledVilkårsresultat(vilkår: List<Vilkår>): List<Vilkårsresultat> {
        return vilkår.groupBy { it.type }.map {
            if (it.key.gjelderFlereBarn()) {
                utledResultatForVilkårSomGjelderFlereBarn(it.value)
            } else {
                it.value.single().resultat
            }
        }
    }

    fun erAlleVilkårOppfylt(
        vilkårsett: List<Vilkår>,
        stønadstype: Stønadstype,
    ): Boolean {
        val inneholderAlleTyperVilkår =
            vilkårsett.map { it.type }.containsAll(VilkårType.hentVilkårForStønad(stønadstype))
        val vilkårsresultat = utledVilkårsresultat(vilkårsett)
        return inneholderAlleTyperVilkår && vilkårsresultat.all { it == Vilkårsresultat.OPPFYLT }
    }
}
