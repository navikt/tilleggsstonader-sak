package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import java.time.LocalDate

data class LagreVilkårReiseOppstartAvslutningHjemreiseDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val adresse: String,
    val reiseId: ReiseId,
    val typeReiseformål: TypeReiseformål,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaReiseOppstartAvslutningHjemreiseDto,
) : LagreVilkår {
    fun tilDomain() =
        LagreVilkårReiseOppstartAvslutningHjemreise(
            fom = fom,
            tom = tom,
            svar = svar.mapValues { it.value.tilDomain() },
            fakta = fakta.mapTilFakta(reiseId = reiseId, adresse = adresse, typeReiseformål = typeReiseformål),
        )
}
