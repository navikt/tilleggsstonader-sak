package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

class MidlertidigOvernattingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.MIDLERTIDIG_OVERNATTING,
        regler =
            setOf(NØDVENDIGE_MERUTGIFTER),
    ) {
    companion object {
        private val NØDVENDIGE_MERUTGIFTER =
            RegelSteg(
                regelId = RegelId.NØDVENDIGE_MERUTGIFTER,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
    }
}
