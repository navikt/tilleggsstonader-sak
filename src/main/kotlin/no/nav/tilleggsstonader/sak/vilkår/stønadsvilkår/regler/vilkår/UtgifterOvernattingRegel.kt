package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

class UtgifterOvernattingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.UTGIFTER_OVERNATTING,
        regler =
            setOf(
                DOKUMENTERT_DELTAKELSE,
                DOKUMENTERT_UTGIFTER_OVERNATTING,
                NØDVENDIGE_MERUTGIFTER,
                HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER,
            ),
    ) {
    companion object {
        private val DOKUMENTERT_DELTAKELSE =
            RegelSteg(
                regelId = RegelId.DOKUMENTERT_DELTAKELSE,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
        private val DOKUMENTERT_UTGIFTER_OVERNATTING =
            RegelSteg(
                regelId = RegelId.DOKUMENTERT_UTGIFTER_OVERNATTING,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
        private val NØDVENDIGE_MERUTGIFTER =
            RegelSteg(
                regelId = RegelId.NØDVENDIGE_MERUTGIFTER,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
        private val HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER =
            RegelSteg(
                regelId = RegelId.HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        SvarId.NEI to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    ),
            )
    }
}
