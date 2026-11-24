package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

/** Respons med en reise eller et paraplyvilkår.  i JSON avgjør hvilken. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(VilkårROAHReiseDto::class, name = "REISEVILKÅR"),
    JsonSubTypes.Type(VilkårMidlertidigBostedDto::class, name = "PARAPLYVILKÅR"),
)
sealed interface VilkårROAHDto

data class VilkårROAHReiseDto(
    val id: VilkårId,
    val globalId: VilkårGlobalId?,
    val fom: LocalDate,
    val tom: LocalDate,
    val reiseId: ReiseId,
    val aktivitetId: VilkårperiodeGlobalId,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<DelvilkårDto>,
    val fakta: FaktaROAHReiseDto,
    val slettetKommentar: String? = null,
    val parentVilkår: VilkårGlobalId,
) : VilkårROAHDto

data class VilkårMidlertidigBostedDto(
    val id: VilkårId,
    val globalId: VilkårGlobalId?,
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitetId: VilkårperiodeGlobalId,
    val resultat: Vilkårsresultat,
    val status: VilkårStatus?,
    val delvilkårsett: List<DelvilkårDto>,
    val fakta: FaktaMidlertidigBostedDto,
    val slettetKommentar: String? = null,
) : VilkårROAHDto
