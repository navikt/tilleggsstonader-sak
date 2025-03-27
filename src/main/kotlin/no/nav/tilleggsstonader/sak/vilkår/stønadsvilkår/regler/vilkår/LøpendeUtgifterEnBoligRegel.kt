package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

class LøpendeUtgifterEnBoligRegel :
    Vilkårsregel(
        vilkårType = VilkårType.LØPENDE_UTGIFTER_EN_BOLIG,
        regler =
            setOf(
                HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE,
                NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET,
                RETT_TIL_BOSTØTTE,
                HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER,
            ),
    ) {
    companion object {
        private val HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE =
            RegelSteg(
                regelId = RegelId.HØYERE_BOUTGIFTER_SAMMENLIGNET_MED_TIDLIGERE,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
        private val NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET =
            RegelSteg(
                regelId = RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
        private val RETT_TIL_BOSTØTTE =
            RegelSteg(
                regelId = RegelId.RETT_TIL_BOSTØTTE,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        SvarId.NEI to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
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
