package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class MålgruppeOvergangsstønadRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE_OVERGANGSSTØNAD,
    regler = setOf(),
    hovedregler = regelIder(),
)
