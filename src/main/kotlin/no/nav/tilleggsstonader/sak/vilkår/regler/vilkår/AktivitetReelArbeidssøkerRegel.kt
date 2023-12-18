package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel

class AktivitetReelArbeidssøkerRegel : Vilkårsregel(
    vilkårType = VilkårType.AKTIVITET_REEL_ARBEIDSSSØKER,
    regler = setOf(),
    hovedregler = setOf(),
)
