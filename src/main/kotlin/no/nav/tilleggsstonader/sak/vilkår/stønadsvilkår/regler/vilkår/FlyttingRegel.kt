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

class FlyttingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.FLYTTING,
        regler =
            setOf(
                HVILKEN_FORBINDELSE_FLYTTER_BRUKER,
                MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET,
                FLYTTER_IFBM_ARBEID_ETTER_AKTIVITET,
                MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID,
                HVORDAN_SKAL_BRUKER_FLYTTE,
            ),
    ) {
    companion object {
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

        private fun måFlytteForAktivitetRegel() =
            RegelSteg(
                regelId = RegelId.MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET,
                erHovedregel = false,
                svarMapping =
                    mapOf(
                        SvarId.FLYTTER_TIL_AKTIVITETSSTED to
                            NesteRegel(HVORDAN_SKAL_BRUKER_FLYTTE.regelId, BegrunnelseType.PÅKREVD),
                        SvarId.FLYTTER_TIL_HJEMSTED to
                            NesteRegel(HVORDAN_SKAL_BRUKER_FLYTTE.regelId, BegrunnelseType.PÅKREVD),
                        SvarId.NEI to
                            SluttSvarRegel(
                                resultat = Resultat.IKKE_OPPFYLT,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_UBESTEMT,
                            ),
                    ),
            )

        private val MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET = måFlytteForAktivitetRegel()
        private val FLYTTER_IFBM_ARBEID_ETTER_AKTIVITET =
            måFlytteRegel(RegelId.FLYTTER_IFBM_ARBEID_ETTER_AKTIVITET)
        private val MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID = måFlytteRegel(RegelId.MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID)

        private fun måFlytteRegel(regelId: RegelId) =
            RegelSteg(
                regelId = regelId,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(HVORDAN_SKAL_BRUKER_FLYTTE.regelId, BegrunnelseType.PÅKREVD),
                        hvisNei =
                            SluttSvarRegel(
                                resultat = Resultat.IKKE_OPPFYLT,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_UBESTEMT,
                            ),
                    ),
            )

        private val HVILKEN_FORBINDELSE_FLYTTER_BRUKER =
            RegelSteg(
                regelId = RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.FLYTTING_VED_OPPSTART_ELLER_AVSLUTNING_AV_TILTAK_ELLER_UTDANNING to
                            NesteRegel(MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET.regelId, BegrunnelseType.PÅKREVD),
                        SvarId.ARBEID_ETTER_GJENNOMFØRT_AKTIVITET to
                            NesteRegel(FLYTTER_IFBM_ARBEID_ETTER_AKTIVITET.regelId, BegrunnelseType.PÅKREVD),
                        SvarId.ENSLIG_FORSØRGER_ELLER_GJENLEVENDE_STARTER_I_ARBEID to
                            NesteRegel(MÅ_FLYTTE_FOR_Å_STARTE_I_ARBEID.regelId, BegrunnelseType.PÅKREVD),
                    ),
            )
    }
}
