package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class MålgruppeAAPFerdigAvklartRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE_AAP_FERDIG_AVKLART,
    regler = setOf(NEDSATT_ARBEIDSEVNE),
    hovedregler = regelIder(NEDSATT_ARBEIDSEVNE),
)
