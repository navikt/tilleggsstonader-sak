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
 * 1. [HVILKEN_FORBINDELSE_FLYTTER_BRUKER]
 *    - start/avsluttet aktivitet -> [MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET]
 *    - arbeid etter gjennomført tiltak/utdanning -> [SAMMENHENG_FLYTTING_OG_ARBEID]
 *    - arbeidssøker -> [FLYTTER_PGA_OPPSTART_ARBEID]
 * 2. Ja -> [ER_KOSTNADER_TIL_FLYTTING_NØDVENDIGE], nei -> ikke oppfylt
 * 3. Ja -> [HVORDAN_SKAL_BRUKER_FLYTTE], nei -> ikke oppfylt
 * 4. Flyttemåte -> oppfylt, og bestemmer hvilke fakta som skal registreres
 */
class FlyttingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.FLYTTING,
        regler =
            setOf(
                HVILKEN_FORBINDELSE_FLYTTER_BRUKER,
                MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET,
                SAMMENHENG_FLYTTING_OG_ARBEID,
                FLYTTER_PGA_OPPSTART_ARBEID,
                ER_KOSTNADER_TIL_FLYTTING_NØDVENDIGE,
                HVORDAN_SKAL_BRUKER_FLYTTE,
            ),
    ) {
    companion object {
        private val IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE =
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

        private val ER_KOSTNADER_TIL_FLYTTING_NØDVENDIGE =
            RegelSteg(
                regelId = RegelId.ER_KOSTNADER_TIL_FLYTTING_NØDVENDIGE,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(HVORDAN_SKAL_BRUKER_FLYTTE.regelId, BegrunnelseType.VALGFRI),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET =
            RegelSteg(
                regelId = RegelId.MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(ER_KOSTNADER_TIL_FLYTTING_NØDVENDIGE.regelId, BegrunnelseType.PÅKREVD),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        // TODO(flytting): Placeholder-spørsmål, se RegelId.SAMMENHENG_FLYTTING_OG_ARBEID
        private val SAMMENHENG_FLYTTING_OG_ARBEID =
            RegelSteg(
                regelId = RegelId.SAMMENHENG_FLYTTING_OG_ARBEID,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(ER_KOSTNADER_TIL_FLYTTING_NØDVENDIGE.regelId, BegrunnelseType.PÅKREVD),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        // TODO(flytting): Placeholder. Svaralternativer og videre flyt for arbeidssøkere er ikke avklart.
        private val FLYTTER_PGA_OPPSTART_ARBEID =
            RegelSteg(
                regelId = RegelId.FLYTTER_PGA_OPPSTART_ARBEID,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val HVILKEN_FORBINDELSE_FLYTTER_BRUKER =
            RegelSteg(
                regelId = RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER,
                erHovedregel = true,
                svarMapping =
                    mapOf(
                        SvarId.FLYTTING_VED_START_AV_AKTIVITET to
                            NesteRegel(MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET.regelId, BegrunnelseType.VALGFRI),
                        SvarId.FLYTTING_VED_AVSLUTTET_AKTIVITET to
                            NesteRegel(MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET.regelId, BegrunnelseType.VALGFRI),
                        SvarId.ARBEID_ETTER_GJENNOMFØRT_AKTIVITET to
                            NesteRegel(SAMMENHENG_FLYTTING_OG_ARBEID.regelId, BegrunnelseType.VALGFRI),
                        SvarId.FLYTTING_ARBEIDSSØKER to
                            NesteRegel(FLYTTER_PGA_OPPSTART_ARBEID.regelId, BegrunnelseType.VALGFRI),
                    ),
            )
    }
}
