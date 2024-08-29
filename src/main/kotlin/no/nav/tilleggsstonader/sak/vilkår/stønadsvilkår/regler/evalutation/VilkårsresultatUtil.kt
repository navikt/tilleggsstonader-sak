package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat

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

    private fun utledResultatForVilkårSomGjelderFlereBarn(value: List<Vilkår>): Vilkårsresultat {
        feilHvis(value.any { !it.type.gjelderFlereBarn() }) {
            "Denne metoden kan kun kalles med vilkår som kan ha flere barn"
        }
        return when {
            value.any { it.resultat == Vilkårsresultat.OPPFYLT } -> Vilkårsresultat.OPPFYLT
            value.all { it.barnId == null && it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL } -> Vilkårsresultat.SKAL_IKKE_VURDERES // Dersom man ikke har barn på behandlingen så er ikke disse vilkårene aktuelle å vurdere
            value.any { it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL } -> Vilkårsresultat.IKKE_TATT_STILLING_TIL
            value.all { it.resultat == Vilkårsresultat.SKAL_IKKE_VURDERES } -> Vilkårsresultat.SKAL_IKKE_VURDERES
            value.any { it.resultat == Vilkårsresultat.IKKE_OPPFYLT } &&
                value.all { it.resultat == Vilkårsresultat.IKKE_OPPFYLT || it.resultat == Vilkårsresultat.SKAL_IKKE_VURDERES } ->
                Vilkårsresultat.IKKE_OPPFYLT

            else -> throw Feil(
                "Utled resultat for aleneomsorg - kombinasjon av resultat er ikke behandlet: " +
                    "${value.map { it.resultat }}",
            )
        }
    }
}
