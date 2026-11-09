package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/** Respons med en reise eller et paraplyvilkår.  i JSON avgjør hvilken. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(VilkårReiseOppstartAvslutningHjemreiseDto::class, name = "REISE"),
    JsonSubTypes.Type(VilkårMidlertidigBostedDto::class, name = "MIDLERTIDIG_BOSTED"),
)
sealed interface VilkårROAHDto
