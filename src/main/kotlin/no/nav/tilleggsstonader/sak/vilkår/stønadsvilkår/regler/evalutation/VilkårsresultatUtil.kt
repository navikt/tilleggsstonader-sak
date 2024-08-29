package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
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
}
