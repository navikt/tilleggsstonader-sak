package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat

fun List<Vilkår>.medResultatFraParent(): List<Vilkår> {
    val vilkårPerGlobalId = mapNotNull { vilkår -> vilkår.globalId?.let { it to vilkår } }.toMap()

    return map { vilkår ->
        val parentVilkår = vilkår.parentVilkår ?: return@map vilkår
        val parentErOppfylt = vilkårPerGlobalId[parentVilkår]?.resultat == Vilkårsresultat.OPPFYLT

        if (parentErOppfylt) vilkår else vilkår.copy(resultat = Vilkårsresultat.IKKE_OPPFYLT)
    }
}
