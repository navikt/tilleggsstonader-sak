package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object ReiseTilSamlingRegelTestUtil {
    fun oppfylteSvarReiseTilSamlingOffentligTransportDto() =
        mapOf(
            RegelId.HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.ER_SAMLING_OBLIGATORISK to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "obligatorisk"),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.DOKUMENTERTE_UTGIFTER to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    fun oppfylteSvarReiseTilSamlingOffentligTransport() =
        oppfylteSvarReiseTilSamlingOffentligTransportDto().mapValues { it.value.tilDomene() }

    fun oppfylteSvarReiseTilSamlingPrivatBilDto() =
        mapOf(
            RegelId.HAR_UTGIFTER_TIL_REISEN to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.ER_SAMLING_OBLIGATORISK to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "obligatorisk"),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to
                SvarOgBegrunnelseDto(
                    svar = SvarId.NEI,
                    begrunnelse = "begrunnelse",
                ),
            RegelId.KAN_REISE_MED_EGEN_BIL to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    fun oppfylteSvarReiseTilSamlingPrivatBil() = oppfylteSvarReiseTilSamlingPrivatBilDto().mapValues { it.value.tilDomene() }

    fun oppfylteDelvilkårReiseTilSamlingOffentligTransport() =
        listOf(
            Delvilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                vurderinger =
                    listOf(
                        Vurdering(
                            regelId = RegelId.HAR_UTGIFTER_TIL_REISEN,
                            svar = SvarId.JA,
                        ),
                        Vurdering(
                            regelId = RegelId.AVSTAND_OVER_TRETTI_KM,
                            svar = SvarId.JA,
                            begrunnelse = "antall km",
                        ),
                        Vurdering(
                            regelId = RegelId.ER_SAMLING_OBLIGATORISK,
                            svar = SvarId.JA,
                            begrunnelse = "obligatorisk",
                        ),
                        Vurdering(
                            regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                            svar = SvarId.JA,
                        ),
                        Vurdering(
                            regelId = RegelId.DOKUMENTERTE_UTGIFTER,
                            svar = SvarId.JA,
                        ),
                    ),
            ),
        )

    fun oppfylteDelvilkårReiseTilSamlingPrivatBil() =
        listOf(
            Delvilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                vurderinger =
                    listOf(
                        Vurdering(
                            regelId = RegelId.HAR_UTGIFTER_TIL_REISEN,
                            svar = SvarId.JA,
                        ),
                        Vurdering(
                            regelId = RegelId.AVSTAND_OVER_TRETTI_KM,
                            svar = SvarId.JA,
                            begrunnelse = "antall km",
                        ),
                        Vurdering(
                            regelId = RegelId.ER_SAMLING_OBLIGATORISK,
                            svar = SvarId.JA,
                            begrunnelse = "obligatorisk",
                        ),
                        Vurdering(
                            regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                            svar = SvarId.NEI,
                        ),
                        Vurdering(
                            regelId = RegelId.KAN_REISE_MED_EGEN_BIL,
                            svar = SvarId.JA,
                        ),
                    ),
            ),
        )

    fun ikkeOppfylteDelvilkårReiseTilSamling() =
        listOf(
            Delvilkår(
                resultat = Vilkårsresultat.IKKE_OPPFYLT,
                vurderinger =
                    listOf(
                        Vurdering(
                            regelId = RegelId.HAR_UTGIFTER_TIL_REISEN,
                            svar = SvarId.JA,
                        ),
                        Vurdering(
                            regelId = RegelId.AVSTAND_OVER_TRETTI_KM,
                            svar = SvarId.JA,
                            begrunnelse = "antall km",
                        ),
                        Vurdering(
                            regelId = RegelId.ER_SAMLING_OBLIGATORISK,
                            svar = SvarId.NEI,
                            begrunnelse = "Ingen dokumentasjon sendt inn, og frist for svar har gått ut.",
                        ),
                    ),
            ),
        )
}
