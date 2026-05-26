package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Resultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
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
                KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                DOKUMENTERTE_UTGIFTER,
                KAN_REISE_MED_EGEN_BIL,
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
        private val KAN_REISE_MED_EGEN_BIL =
            RegelSteg(
                regelId = RegelId.KAN_REISE_MED_EGEN_BIL,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.UTEN,
                                tilhørendeFaktaType = TypeVilkårFakta.REISE_TIL_SAMLING_PRIVAT_BIL,
                            ),
                        hvisNei = IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
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
        private val KAN_REISE_MED_OFFENTLIG_TRANSPORT =
            RegelSteg(
                regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.UTEN,
                                tilhørendeFaktaType = TypeVilkårFakta.REISE_TIL_SAMLING_OFFENTLIG_TRANSPORT,
                            ),
                        hvisNei = NesteRegel(KAN_REISE_MED_EGEN_BIL.regelId, BegrunnelseType.UTEN),
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
