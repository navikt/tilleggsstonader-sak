package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class EksempelRegel : Vilkårsregel(
    vilkårType = VilkårType.EKSEMPEL,
    regler = setOf(HAR_ET_NAVN),
    hovedregler = regelIder(HAR_ET_NAVN),
) {

    companion object {

        private val HAR_ET_NAVN =
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN,
                svarMapping = jaNeiSvarRegel(hvisJa = OPPFYLT_MED_PÅKREVD_BEGRUNNELSE),
            )
    }
}
