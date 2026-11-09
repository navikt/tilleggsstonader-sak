package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat

/** Et vilkår med paraply er bare oppfylt når paraplyen er oppfylt. */
fun Vilkår.medResultatFraParent(parent: Vilkår?): Vilkår {
    if (parentVilkår == null) return this

    return if (parent?.resultat == Vilkårsresultat.OPPFYLT) this else copy(resultat = Vilkårsresultat.IKKE_OPPFYLT)
}

fun List<Vilkår>.medResultatFraParent(): List<Vilkår> {
    val vilkårPerGlobalId = mapNotNull { vilkår -> vilkår.globalId?.let { it to vilkår } }.toMap()

    return map { vilkår -> vilkår.medResultatFraParent(vilkårPerGlobalId[vilkår.parentVilkår]) }
}
