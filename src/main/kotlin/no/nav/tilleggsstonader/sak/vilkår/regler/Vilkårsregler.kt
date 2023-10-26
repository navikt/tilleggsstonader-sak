package no.nav.tilleggsstonader.sak.vilkår.regler

import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.AktivitetRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.EksempelRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.MålgruppeRegel

/**
 * Singleton for å holde på alle regler
 */
class Vilkårsregler private constructor(val vilkårsregler: Map<VilkårType, Vilkårsregel>) {

    companion object {

        val ALLE_VILKÅRSREGLER = Vilkårsregler(alleVilkårsregler.associateBy { it.vilkårType })
    }
}

private val alleVilkårsregler = Stønadstype.entries.map { vilkårsreglerForStønad(it) }.flatten()

fun vilkårsreglerForStønad(stønadstype: Stønadstype): List<Vilkårsregel> =
    when (stønadstype) {
        Stønadstype.BARNETILSYN -> listOf(
            MålgruppeRegel(),
            AktivitetRegel(),
        )
    }

fun hentVilkårsregel(vilkårType: VilkårType): Vilkårsregel {
    return Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler[vilkårType]
        ?: error("Finner ikke vilkårsregler for vilkårType=$vilkårType")
}
