package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Resultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

/**
 * Flytting for tiltaksenheten gjelder alltid flytting i forbindelse med arbeid,
 * så spørsmålet om flytteforbindelse fra [FlyttingRegel] er ikke med her.
 */
class FlyttingTsrRegel :
    Vilkårsregel(
        vilkårType = VilkårType.FLYTTING_TSR,
        regler =
            setOf(
                HAR_UTGIFTER_TIL_FLYTTING,
                SØKER_FØR_FLYTTING_ER_GJENNOMFØRT,
                MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID,
                HVORDAN_SKAL_BRUKER_FLYTTE,
            ),
    ) {
    companion object {
        private val ikkeOppfyltUtenFlyttefakta =
            SluttSvarRegel(
                resultat = Resultat.IKKE_OPPFYLT,
                begrunnelseType = BegrunnelseType.PÅKREVD,
                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_UBESTEMT,
            )

        private val HVORDAN_SKAL_BRUKER_FLYTTE =
            RegelSteg(
                regelId = RegelId.HVORDAN_SKAL_BRUKER_FLYTTE,
                erHovedregel = false,
                svarMapping =
                    mapOf(
                        SvarId.FLYTTEBYRÅ to
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_FLYTTEBYRÅ,
                            ),
                        SvarId.FLYTTER_SELV to
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_FLYTTE_SELV,
                            ),
                    ),
            )

        private val MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID =
            RegelSteg(
                regelId = RegelId.MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(HVORDAN_SKAL_BRUKER_FLYTTE.regelId, BegrunnelseType.PÅKREVD),
                        hvisNei = ikkeOppfyltUtenFlyttefakta,
                    ),
            )

        private val SØKER_FØR_FLYTTING_ER_GJENNOMFØRT =
            RegelSteg(
                regelId = RegelId.SØKER_FØR_FLYTTING_ER_GJENNOMFØRT,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID.regelId, BegrunnelseType.VALGFRI),
                        hvisNei = ikkeOppfyltUtenFlyttefakta,
                    ),
            )

        private val HAR_UTGIFTER_TIL_FLYTTING =
            RegelSteg(
                regelId = RegelId.HAR_UTGIFTER_TIL_FLYTTING,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(SØKER_FØR_FLYTTING_ER_GJENNOMFØRT.regelId, BegrunnelseType.VALGFRI),
                        hvisNei =
                            SluttSvarRegel(
                                resultat = Resultat.IKKE_OPPFYLT,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                            ),
                    ),
            )
    }
}
