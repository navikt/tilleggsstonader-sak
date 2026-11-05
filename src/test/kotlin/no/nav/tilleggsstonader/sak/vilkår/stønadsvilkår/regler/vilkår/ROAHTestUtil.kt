package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object ROAHTestUtil {
    fun oppfylteSvarOppstartAvslutningOffentligTransportDto() =
        mapOf(
            RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    fun oppfylteSvarHjemreiseOffentligTransportDto() =
        mapOf(
            RegelId.ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    fun oppfylteSvarMidlertidigBostedDto() =
        mapOf(
            RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.ROAH_HAR_BARN_UNDER_18_ÅR_SOM_FLYTTER_MED to SvarOgBegrunnelseDto(svar = SvarId.NEI),
            RegelId.ROAH_HAR_BARN_IKKE_FULLFØRT_FJERDE_SKOLEÅR_HJEMME to SvarOgBegrunnelseDto(svar = SvarId.NEI),
            RegelId.ROAH_HAR_DOKUMENTERTE_SÆRSKILTE_BEHOV_FLERE_HJEMREISER to SvarOgBegrunnelseDto(svar = SvarId.NEI),
        )

    fun ikkeOppfylteSvarMidlertidigBostedDto() =
        mapOf(
            RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE to SvarOgBegrunnelseDto(svar = SvarId.NEI, begrunnelse = "Test"),
        )
}
