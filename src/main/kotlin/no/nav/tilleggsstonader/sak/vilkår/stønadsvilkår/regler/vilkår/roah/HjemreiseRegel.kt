package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class HjemreiseRegel :
    Vilkårsregel(
        vilkårType = VilkårType.ROAH_HJEMREISE,
        regler =
            setOf(
                ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL,
                ROAH_HJEMREISE_BEHOV_FLERE_REISER,
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

        private val ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN, BegrunnelseType.VALGFRI),
                        hvisNei = NesteRegel(RegelId.ROAH_HJEMREISE_BEHOV_FLERE_REISER, BegrunnelseType.VALGFRI),
                    ),
            )

        private val ROAH_HJEMREISE_BEHOV_FLERE_REISER =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_BEHOV_FLERE_REISER,
                erHovedregel = false,
                svarMapping =
                    mapOf(
                        SvarId.ROAH_HAR_BARN_IKKE_FERDIG_MED_FJERDE_SKOLEÅR to
                            NesteRegel(
                                regelId = RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                            ),
                        SvarId.ROAH_HAR_DOKUMENTERTE_ANDRE_SÆRSKILTE_BEHOV to
                            NesteRegel(
                                regelId = RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                            ),
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
    }
}
