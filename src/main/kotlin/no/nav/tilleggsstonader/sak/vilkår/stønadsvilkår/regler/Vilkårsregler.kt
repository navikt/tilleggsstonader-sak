package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.DagligReiseOffentiligTransportRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.LøpendeUtgifterEnBoligRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.LøpendeUtgifterToBoligerRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.UtgifterOvernattingRegel

/**
 * Singleton for å holde på alle regler
 */
class Vilkårsregler private constructor(
    val vilkårsregler: Map<VilkårType, Vilkårsregel>,
) {
    companion object {
        val ALLE_VILKÅRSREGLER = Vilkårsregler(alleVilkårsregler.associateBy { it.vilkårType })
    }
}

private val alleVilkårsregler: List<Vilkårsregel> =
    Stønadstype.entries.map { vilkårsreglerForStønad(it) }.flatten()

fun vilkårsreglerForStønad(stønadstype: Stønadstype): List<Vilkårsregel> =
    when (stønadstype) {
        Stønadstype.BARNETILSYN ->
            listOf(
                PassBarnRegel(),
            )

        Stønadstype.LÆREMIDLER -> emptyList()
        Stønadstype.BOUTGIFTER ->
            listOf(
                UtgifterOvernattingRegel(),
                LøpendeUtgifterEnBoligRegel(),
                LøpendeUtgifterToBoligerRegel(),
            )

        Stønadstype.DAGLIG_REISE_TSO -> listOf(DagligReiseOffentiligTransportRegel())
        Stønadstype.DAGLIG_REISE_TSR -> listOf(DagligReiseOffentiligTransportRegel())
    }

private val vilkårstyperPerStønad: Map<Stønadstype, Set<VilkårType>> =
    Stønadstype.entries.associateWith { vilkårsreglerForStønad(it).map { it.vilkårType }.toSet() }

fun finnesVilkårTypeForStønadstype(
    stønadstype: Stønadstype,
    vilkårType: VilkårType,
): Boolean {
    val vilkårstyper =
        vilkårstyperPerStønad[stønadstype]
            ?: error("Finner ikke vilkårstyper for stønadstype=$stønadstype")
    return vilkårstyper.contains(vilkårType)
}

fun hentVilkårsregel(vilkårType: VilkårType): Vilkårsregel =
    Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler[vilkårType]
        ?: error("Finner ikke vilkårsregler for vilkårType=$vilkårType")
