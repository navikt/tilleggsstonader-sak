package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel

class AktivitetUtdanningRegel : Vilkårsregel(
    vilkårType = VilkårType.AKTIVITET_UTDANNING,
    regler = setOf(),
    hovedregler = setOf(),
)
