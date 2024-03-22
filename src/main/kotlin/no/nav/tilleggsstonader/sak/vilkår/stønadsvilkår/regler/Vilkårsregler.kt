package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegel

/**
 * Singleton for å holde på alle regler
 */
class Vilkårsregler private constructor(val vilkårsregler: Map<VilkårType, Vilkårsregel>) {

    companion object {

        val ALLE_VILKÅRSREGLER = Vilkårsregler(alleVilkårsregler.associateBy { it.vilkårType })
    }
}

private val alleVilkårsregler: List<Vilkårsregel> =
    Stønadstype.entries.map { vilkårsreglerForStønad(it) }.flatten()

fun vilkårsreglerForStønad(stønadstype: Stønadstype): List<Vilkårsregel> =
    when (stønadstype) {
        Stønadstype.BARNETILSYN -> listOf(
            PassBarnRegel(),
        )
    }

fun hentVilkårsregel(vilkårType: VilkårType): Vilkårsregel {
    return Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler[vilkårType]
        ?: error("Finner ikke vilkårsregler for vilkårType=$vilkårType")
}

fun vilkårsreglerPassBarn() = vilkårsreglerForStønad(Stønadstype.BARNETILSYN).map { it.regler }.first()

fun hovedreglerPassBarn() = vilkårsreglerForStønad(Stønadstype.BARNETILSYN).map { it.hovedregler }.first()

fun finnOppfølgingsregel(regelId: RegelId, svar: SvarId?) =
    vilkårsreglerPassBarn()[regelId]?.svarMapping?.get(svar)?.regelId
