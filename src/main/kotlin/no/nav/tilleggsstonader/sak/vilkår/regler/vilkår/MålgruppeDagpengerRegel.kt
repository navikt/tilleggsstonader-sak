package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class MålgruppeDagpengerRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE_DAGPENGER,
    regler = setOf(NEDSATT_ARBEIDSEVNE),
    hovedregler = regelIder(NEDSATT_ARBEIDSEVNE),
)
