package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class MålgruppeAAPFerdigAvklartRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE_AAP_FERDIG_AVKLART,
    regler = setOf(NEDSATT_ARBEIDSEVNE),
    hovedregler = regelIder(NEDSATT_ARBEIDSEVNE),
) {
    companion object {
        private val NEDSATT_ARBEIDSEVNE =
            RegelSteg(
                regelId = RegelId.NEDSATT_ARBEIDSEVNE,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT,
                ),
            )
    }
}
