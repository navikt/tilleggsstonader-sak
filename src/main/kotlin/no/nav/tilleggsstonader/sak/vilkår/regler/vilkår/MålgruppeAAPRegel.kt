package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel

class MålgruppeAAPRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE_AAP,
    regler = setOf(),
    hovedregler = setOf(),
)
