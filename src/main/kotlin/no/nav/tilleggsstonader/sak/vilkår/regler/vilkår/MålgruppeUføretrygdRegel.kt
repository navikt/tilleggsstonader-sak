package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class MålgruppeUføretrygdRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE_UFØRETRYGD,
    regler = setOf(),
    hovedregler = regelIder(),
)
