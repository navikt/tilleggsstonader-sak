package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

fun VilkårDagligReiseDto.tilLagreDagligReiseDto() =
    LagreDagligReiseDto(
        fom = fom,
        tom = tom,
        adresse = adresse!!,
        svar = delvilkårsett.tilSvar(),
        fakta = fakta,
    )

// TODO
private fun List<DelvilkårDto>.tilSvar() =
    mapOf(
        RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
        RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
    )
