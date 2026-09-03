package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object ReiseTilSamlingRegelTestUtil {
    fun oppfylteSvarReiseTilSamlingOffentligTransportDto() =
        mapOf(
            RegelId.HAR_NØDVENDIGE_UTGIFTER_TIL_REISE_TIL_SAMLING to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.ER_SAMLING_OBLIGATORISK to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.DOKUMENTERTE_UTGIFTER to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    fun oppfylteSvarReiseTilSamlingOffentligTransport() =
        oppfylteSvarReiseTilSamlingOffentligTransportDto().mapValues { it.value.tilDomain() }

    fun oppfylteSvarReiseTilSamlingPrivatBilDto() =
        mapOf(
            RegelId.HAR_NØDVENDIGE_UTGIFTER_TIL_REISE_TIL_SAMLING to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.ER_SAMLING_OBLIGATORISK to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to
                SvarOgBegrunnelseDto(
                    svar = SvarId.NEI,
                    begrunnelse = "begrunnelse",
                ),
            RegelId.KAN_REISE_MED_EGEN_BIL to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    fun oppfylteSvarReiseTilSamlingPrivatBil() = oppfylteSvarReiseTilSamlingPrivatBilDto().mapValues { it.value.tilDomain() }
}
