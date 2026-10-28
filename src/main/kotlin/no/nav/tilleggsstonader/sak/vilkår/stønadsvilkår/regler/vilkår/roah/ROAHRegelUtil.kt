package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Resultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål

object ROAHRegelUtil {
    fun finnRelevantRegel(typeReiseformål: TypeReiseformål): Vilkårsregel =
        when (typeReiseformål) {
            TypeReiseformål.OPPSTART, TypeReiseformål.AVSLUTNING -> OppstartAvslutningRegel()
            TypeReiseformål.HJEMREISE -> HjemreiseRegel()
        }

    fun harUtgifterTilReisenRegel(erHovedregel: Boolean) =
        RegelSteg(
            regelId = RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN,
            erHovedregel = erHovedregel,
            svarMapping =
                jaNeiSvarRegel(
                    hvisJa = NesteRegel(RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT, BegrunnelseType.VALGFRI),
                    hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
        )

    fun kanReiseMedOffentligTransportRegel() =
        RegelSteg(
            regelId = RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT,
            erHovedregel = false,
            svarMapping =
                jaNeiSvarRegel(
                    hvisJa = NesteRegel(RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN, BegrunnelseType.VALGFRI),
                    hvisNei = NesteRegel(RegelId.ROAH_KAN_BENYTTE_PRIVAT_BIL, BegrunnelseType.PÅKREVD),
                ),
        )

    fun harDokumenterteUtgifterTilReisenRegel() =
        RegelSteg(
            regelId = RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN,
            erHovedregel = false,
            svarMapping =
                jaNeiSvarRegel(
                    hvisJa =
                        SluttSvarRegel(
                            resultat = Resultat.OPPFYLT,
                            begrunnelseType = BegrunnelseType.VALGFRI,
                            tilhørendeFaktaType = TypeVilkårFakta.REISE_OPPSTART_AVSLUTNING_HJEMREISE_OFFENTLIG_TRANSPORT,
                        ),
                    hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
        )

    fun kanBenyttePrivatBilRegel() =
        RegelSteg(
            regelId = RegelId.ROAH_KAN_BENYTTE_PRIVAT_BIL,
            erHovedregel = false,
            svarMapping =
                jaNeiSvarRegel(
                    hvisJa =
                        SluttSvarRegel(
                            resultat = Resultat.OPPFYLT,
                            begrunnelseType = BegrunnelseType.VALGFRI,
                            tilhørendeFaktaType = TypeVilkårFakta.REISE_OPPSTART_AVSLUTNING_HJEMREISE_PRIVAT_BIL,
                        ),
                    hvisNei = NesteRegel(RegelId.ROAH_BENYTTET_TAXI, BegrunnelseType.PÅKREVD),
                ),
        )

    fun benyttetTaxiRegel() =
        RegelSteg(
            regelId = RegelId.ROAH_BENYTTET_TAXI,
            erHovedregel = false,
            svarMapping =
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel(Resultat.OPPFYLT, BegrunnelseType.VALGFRI),
                    hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
        )
}
