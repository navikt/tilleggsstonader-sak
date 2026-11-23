package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingTsrRegel

val vilkårTyperFlytting = setOf(VilkårType.FLYTTING, VilkårType.FLYTTING_TSR)

fun vilkårTypeFlytting(stønadstype: Stønadstype): VilkårType =
    when (stønadstype) {
        Stønadstype.FLYTTING_TSO -> VilkårType.FLYTTING
        Stønadstype.FLYTTING_TSR -> VilkårType.FLYTTING_TSR
        else -> feil("Stønadstype=$stønadstype har ikke flyttevilkår")
    }

fun flyttingRegel(stønadstype: Stønadstype): Vilkårsregel =
    when (stønadstype) {
        Stønadstype.FLYTTING_TSO -> FlyttingRegel()
        Stønadstype.FLYTTING_TSR -> FlyttingTsrRegel()
        else -> feil("Stønadstype=$stønadstype har ikke flyttevilkår")
    }
