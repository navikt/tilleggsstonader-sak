package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class ReiseTilSamlingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.REISE_TIL_SAMLING,
        regler =
            setOf(
                AVSTAND_OVER_TRETTI_KM,
                DOKUMENTERTE_UTGIFTER,
                DEKKET_AV_ANNET_STIPEND,
            ),
    ) {
    companion object {
        private val DEKKET_AV_ANNET_STIPEND =
            RegelSteg(
                regelId = RegelId.DEKKET_AV_ANNET_STIPEND,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        hvisNei = OPPFYLT,
                    ),
            )
        private val DOKUMENTERTE_UTGIFTER =
            RegelSteg(
                regelId = RegelId.DOKUMENTERTE_UTGIFTER,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(DEKKET_AV_ANNET_STIPEND.regelId, BegrunnelseType.UTEN),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
        private val AVSTAND_OVER_TRETTI_KM =
            RegelSteg(
                regelId = RegelId.AVSTAND_OVER_TRETTI_KM,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(DOKUMENTERTE_UTGIFTER.regelId, BegrunnelseType.PÅKREVD),
                        hvisNei =
                        IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
    }
}
