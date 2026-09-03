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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class ReiseTilSamlingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.REISE_TIL_SAMLING,
        regler =
            setOf(
                HAR_NØDVENDIGE_UTGIFTER_TIL_REISE,
                ER_SAMLING_OBLIGATORISK,
                AVSTAND_OVER_TRETTI_KM,
                KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                DOKUMENTERTE_UTGIFTER,
                KAN_REISE_MED_EGEN_BIL,
            ),
    ) {
    companion object {
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
                        hvisNei =
                            SluttSvarRegel(
                                resultat = Resultat.IKKE_OPPFYLT,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                            ),
                    ),
            )

        private val DOKUMENTERTE_UTGIFTER =
            RegelSteg(
                regelId = RegelId.DOKUMENTERTE_UTGIFTER,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.UTEN,
                                tilhørendeFaktaType = TypeVilkårFakta.REISE_TIL_SAMLING_OFFENTLIG_TRANSPORT,
                            ),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val KAN_REISE_MED_OFFENTLIG_TRANSPORT =
            RegelSteg(
                regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.DOKUMENTERTE_UTGIFTER, BegrunnelseType.VALGFRI),
                        hvisNei = NesteRegel(KAN_REISE_MED_EGEN_BIL.regelId, BegrunnelseType.PÅKREVD),
                    ),
            )

        private val ER_SAMLING_OBLIGATORISK =
            RegelSteg(
                regelId = RegelId.ER_SAMLING_OBLIGATORISK,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(KAN_REISE_MED_OFFENTLIG_TRANSPORT.regelId, BegrunnelseType.VALGFRI),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val AVSTAND_OVER_TRETTI_KM =
            RegelSteg(
                regelId = RegelId.AVSTAND_OVER_TRETTI_KM,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(ER_SAMLING_OBLIGATORISK.regelId, BegrunnelseType.PÅKREVD),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val HAR_NØDVENDIGE_UTGIFTER_TIL_REISE =
            RegelSteg(
                regelId = RegelId.HAR_NØDVENDIGE_UTGIFTER_TIL_REISE_TIL_SAMLING,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            NesteRegel(
                                regelId = AVSTAND_OVER_TRETTI_KM.regelId,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                            ),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
    }
}
