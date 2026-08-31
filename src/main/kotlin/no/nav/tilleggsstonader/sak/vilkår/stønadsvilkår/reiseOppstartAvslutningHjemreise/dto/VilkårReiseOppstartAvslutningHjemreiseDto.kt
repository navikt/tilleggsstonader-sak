package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import java.time.LocalDate

data class VilkårReiseOppstartAvslutningHjemreiseDto(
    val id: VilkårId,
    val fom: LocalDate,
    val tom: LocalDate,
    val adresse: String? = null,
    val reiseId: ReiseId,
    val typeReiseformål: TypeReiseformål,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<DelvilkårDto>,
    val fakta: FaktaReiseOppstartAvslutningHjemreiseDto,
    val slettetKommentar: String? = null,
)
