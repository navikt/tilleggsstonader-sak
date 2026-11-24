package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingTsrRegel
import java.time.LocalDate

data class VilkårFlytting(
    val id: VilkårId = VilkårId.random(),
    val globalId: VilkårGlobalId? = VilkårGlobalId.random(),
    val behandlingId: BehandlingId,
    val vilkårType: VilkårType,
    val flyttedato: LocalDate,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<Delvilkår>,
    val fakta: FlyttingVilkårFakta,
    val slettetKommentar: String? = null,
    val opphavsvilkår: Opphavsvilkår? = null,
) : Periode<LocalDate> {
    init {
        require(vilkårType in vilkårTyperFlytting) { "Ugyldig vilkårstype=$vilkårType for flytting" }
    }

    override val fom = flyttedato
    override val tom = flyttedato
}

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
