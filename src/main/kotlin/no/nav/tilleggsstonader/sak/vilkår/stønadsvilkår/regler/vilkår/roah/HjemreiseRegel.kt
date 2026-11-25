package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class HjemreiseRegel :
    Vilkårsregel(
        vilkårType = VilkårType.ROAH_HJEMREISE,
        regler =
            setOf(
                ROAH_HJEMREISE_HAR_REISER_IGJEN_AV_KVOTE,
                ROAH_HJEMREISE_SÆRLIG_BEHOV_UTENFOR_KVOTE,
                ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_UTENFOR_KVOTE,
                ROAH_HJEMREISE_SÆRLIG_BEHOV_INNENFOR_KVOTE,
                ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_INNENFOR_KVOTE,
                ROAH_HAR_UTGIFTER_TIL_REISEN,
                ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN,
                ROAH_KAN_BENYTTE_PRIVAT_BIL,
                ROAH_BENYTTET_TAXI,
            ),
    ) {
    companion object {
        private val ROAH_HAR_UTGIFTER_TIL_REISEN = ROAHRegelUtil.harUtgifterTilReisenRegel(erHovedregel = false)
        private val ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT = ROAHRegelUtil.kanReiseMedOffentligTransportRegel()
        private val ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN = ROAHRegelUtil.harDokumenterteUtgifterTilReisenRegel()
        private val ROAH_KAN_BENYTTE_PRIVAT_BIL = ROAHRegelUtil.kanBenyttePrivatBilRegel()
        private val ROAH_BENYTTET_TAXI = ROAHRegelUtil.benyttetTaxiRegel()

        private val ROAH_HJEMREISE_HAR_REISER_IGJEN_AV_KVOTE =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_HAR_REISER_IGJEN_AV_KVOTE,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_INNENFOR_KVOTE, BegrunnelseType.VALGFRI),
                        hvisNei = NesteRegel(RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_UTENFOR_KVOTE, BegrunnelseType.VALGFRI),
                    ),
            )

        // Ingen reiser igjen av kvoten: reisen må begrunnes i et dokumentert særlig behov
        private val ROAH_HJEMREISE_SÆRLIG_BEHOV_UTENFOR_KVOTE =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_UTENFOR_KVOTE,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            NesteRegel(
                                RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_UTENFOR_KVOTE,
                                BegrunnelseType.PÅKREVD,
                            ),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_UTENFOR_KVOTE =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_UTENFOR_KVOTE,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN, BegrunnelseType.PÅKREVD),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        // Reiser igjen av kvoten: særlig behov avgjør bare om reisen trekkes fra kvoten
        private val ROAH_HJEMREISE_SÆRLIG_BEHOV_INNENFOR_KVOTE =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_INNENFOR_KVOTE,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            NesteRegel(
                                RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_INNENFOR_KVOTE,
                                BegrunnelseType.PÅKREVD,
                            ),
                        hvisNei = NesteRegel(RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN, BegrunnelseType.VALGFRI),
                    ),
            )

        private val ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_INNENFOR_KVOTE =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_SÆRLIG_BEHOV_DOKUMENTERT_INNENFOR_KVOTE,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN, BegrunnelseType.PÅKREVD),
                        hvisNei = NesteRegel(RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN, BegrunnelseType.VALGFRI),
                    ),
            )
    }
}
