package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.libs.feil.brukerfeil
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårROAHReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(LagreVilkårROAHReiseDto::class, name = "REISEVILKÅR"),
    JsonSubTypes.Type(LagreVilkårMidlertidigBostedDto::class, name = "PARAPLYVILKÅR"),
)
sealed interface LagreVilkårROAHDto : LagreVilkår {
    fun tilDomene(): LagreVilkårROAH
}

data class LagreVilkårROAHReiseDto(
    val fom: LocalDate,
    val tom: LocalDate?,
    val reiseId: ReiseId,
    val aktivitetId: VilkårperiodeGlobalId,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaROAHReiseDto,
    val parentVilkår: VilkårGlobalId,
) : LagreVilkårROAHDto {
    override fun tilDomene() =
        LagreVilkårROAHReise(
            fom = fom,
            tom = utledTom(),
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            svar = svar.mapValues { it.value.tilDomene() },
            fakta = fakta.mapTilFakta(),
            parentVilkår = parentVilkår,
        )

    private fun utledTom(): LocalDate =
        when (val typeReiseformål = fakta.typeReiseformål) {
            TypeReiseformål.HJEMREISE -> tom ?: brukerfeil("tom må settes for hjemreise")
            TypeReiseformål.OPPSTART, TypeReiseformål.AVSLUTNING -> {
                brukerfeilHvis(tom != null && tom != fom) { "Tom kan ikke settes til noe annet enn lik fom for $typeReiseformål" }
                fom
            }
        }
}

data class LagreVilkårMidlertidigBostedDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val aktivitetId: VilkårperiodeGlobalId,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaMidlertidigBostedDto,
) : LagreVilkårROAHDto {
    override fun tilDomene() =
        LagreVilkårMidlertidigBosted(
            fom = fom,
            tom = tom,
            aktivitetId = aktivitetId,
            fakta = fakta.mapTilFakta(),
            svar = svar.mapValues { it.value.tilDomene() },
        )
}
