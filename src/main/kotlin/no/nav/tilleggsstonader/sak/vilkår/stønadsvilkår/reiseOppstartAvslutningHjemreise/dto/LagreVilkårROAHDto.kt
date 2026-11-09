package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårROAH

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(LagreVilkårReiseOppstartAvslutningHjemreiseDto::class, name = "REISE"),
    JsonSubTypes.Type(LagreVilkårMidlertidigBostedDto::class, name = "MIDLERTIDIG_BOSTED"),
)
sealed interface LagreVilkårROAHDto : LagreVilkår {
    fun tilDomene(): LagreVilkårROAH
}
