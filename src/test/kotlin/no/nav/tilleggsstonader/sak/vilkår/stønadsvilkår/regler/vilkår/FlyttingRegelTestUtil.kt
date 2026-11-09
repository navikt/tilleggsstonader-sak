package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping.ByggVilkårFraSvar

object FlyttingRegelTestUtil {
    fun oppfylteSvarFlyttebyråDto() = oppfylteSvarFlyttingDto(SvarId.FLYTTEBYRÅ)

    fun oppfylteSvarFlytteSelvDto() = oppfylteSvarFlyttingDto(SvarId.FLYTTER_SELV)

    fun oppfylteDelvilkårFlyttebyrå() = byggDelvilkår(oppfylteSvarFlyttebyråDto())

    fun oppfylteDelvilkårFlytteSelv() = byggDelvilkår(oppfylteSvarFlytteSelvDto())

    fun ikkeOppfylteDelvilkårFlytting() =
        byggDelvilkår(
            mapOf(
                RegelId.HAR_UTGIFTER_TIL_FLYTTING to SvarOgBegrunnelseDto(SvarId.JA),
                RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                    SvarOgBegrunnelseDto(
                        SvarId.FLYTTING_VED_OPPSTART_ELLER_AVSLUTNING_AV_TILTAK_ELLER_UTDANNING,
                        "Syntetisk begrunnelse",
                    ),
                RegelId.MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET to
                    SvarOgBegrunnelseDto(SvarId.NEI, "Flytting er ikke nødvendig for å gjennomføre aktiviteten."),
            ),
        )

    private fun oppfylteSvarFlyttingDto(flyttemåte: SvarId) =
        mapOf(
            RegelId.HAR_UTGIFTER_TIL_FLYTTING to SvarOgBegrunnelseDto(SvarId.JA),
            RegelId.HVILKEN_FORBINDELSE_FLYTTER_BRUKER to
                SvarOgBegrunnelseDto(
                    SvarId.FLYTTING_VED_OPPSTART_ELLER_AVSLUTNING_AV_TILTAK_ELLER_UTDANNING,
                    "Syntetisk begrunnelse",
                ),
            RegelId.MÅ_FLYTTE_FOR_Å_GJENNOMFØRE_AKTIVITET to
                SvarOgBegrunnelseDto(SvarId.FLYTTER_TIL_AKTIVITETSSTED, "Syntetisk begrunnelse"),
            RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelseDto(flyttemåte),
        )

    private fun byggDelvilkår(svar: Map<RegelId, SvarOgBegrunnelseDto>) =
        ByggVilkårFraSvar.byggDelvilkårsettFraSvarOgVilkårsregel(
            vilkårsregel = FlyttingRegel(),
            svar = svar.mapValues { it.value.tilDomene() },
        )
}
