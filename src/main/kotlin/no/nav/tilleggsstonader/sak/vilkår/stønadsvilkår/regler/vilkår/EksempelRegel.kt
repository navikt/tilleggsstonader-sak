package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.regelIder

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
