package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel

class OppstartAvslutningRegel :
    Vilkårsregel(
        vilkårType = VilkårType.ROAH_OPPSTART_AVSLUTNING,
        regler =
            setOf(
                ROAH_HAR_UTGIFTER_TIL_REISEN,
                ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN,
                ROAH_KAN_BENYTTE_PRIVAT_BIL,
                ROAH_BENYTTET_TAXI,
            ),
    ) {
    companion object {
        private val ROAH_HAR_UTGIFTER_TIL_REISEN = ROAHRegelUtil.harUtgifterTilReisenRegel(erHovedregel = true)
        private val ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT = ROAHRegelUtil.kanReiseMedOffentligTransportRegel()
        private val ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN = ROAHRegelUtil.harDokumenterteUtgifterTilReisenRegel()
        private val ROAH_KAN_BENYTTE_PRIVAT_BIL = ROAHRegelUtil.kanBenyttePrivatBilRegel()
        private val ROAH_BENYTTET_TAXI = ROAHRegelUtil.benyttetTaxiRegel()
    }
}
