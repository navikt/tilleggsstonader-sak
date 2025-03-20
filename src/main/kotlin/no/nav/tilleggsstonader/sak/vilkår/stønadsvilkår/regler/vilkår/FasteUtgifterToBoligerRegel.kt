package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

class FasteUtgifterToBoligerRegel :
    Vilkårsregel(
        vilkårType = VilkårType.FASTE_UTGIFTER_TO_BOLIGER,
        regler =
            setOf(
                NØDVENDING_Å_BO_NÆRMERE_AKTIVITET,
                DOKUMENTERT_UTGIFTER_BOLIG,
                HØYERE_UTGIFTER_HELSEMESSIG_ÅRSAKER,
            ),
    ) {
    companion object {
        private val NØDVENDING_Å_BO_NÆRMERE_AKTIVITET =
            RegelSteg(
                regelId = RegelId.NØDVENDIG_Å_BO_NÆRMERE_AKTIVITET,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val DOKUMENTERT_UTGIFTER_BOLIG =
            RegelSteg(
                regelId = RegelId.DOKUMENTERT_UTGIFTER_BOLIG,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
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
