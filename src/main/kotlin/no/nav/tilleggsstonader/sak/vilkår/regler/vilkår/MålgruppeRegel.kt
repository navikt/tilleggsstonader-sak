package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class MålgruppeRegel : Vilkårsregel(
    vilkårType = VilkårType.MÅLGRUPPE,
    regler = setOf(HOVEDYTELSE),
    hovedregler = regelIder(HOVEDYTELSE),
) {
    companion object {
        private val HOVEDYTELSE =
            RegelSteg(
                regelId = RegelId.MÅLGRUPPE,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )
    }
}
