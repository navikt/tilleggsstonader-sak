package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId

fun VilkårDagligReiseDto.tilLagreDagligReiseDto() =
    LagreDagligReiseDto(
        fom = fom,
        tom = tom,
        adresse = adresse ?: error("Det er påkrevd å sende inn adresse når reisevilkår opprettes"),
        reiseId = reiseId,
        svar = delvilkårsett.tilSvar(),
        fakta = fakta,
    )

private fun List<DelvilkårDto>.tilSvar(): Map<RegelId, SvarOgBegrunnelseDto> =
    this
        .flatMap { it.vurderinger }
        .associate { vurderingDto ->
            vurderingDto.regelId to
                SvarOgBegrunnelseDto(
                    svar = vurderingDto.svar ?: error("Forventer svar i test dataen"),
                    begrunnelse = vurderingDto.begrunnelse,
                )
        }
