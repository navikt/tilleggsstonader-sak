package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.libs.feil.brukerfeil
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class LagreVilkårReiseOppstartAvslutningHjemreiseDto(
    val fom: LocalDate,
    val tom: LocalDate?,
    val reiseId: ReiseId,
    val aktivitetId: VilkårperiodeGlobalId,
    val typeReiseformål: TypeReiseformål,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaReiseOppstartAvslutningHjemreiseDto,
) : LagreVilkår {
    fun tilDomene() =
        LagreVilkårReiseOppstartAvslutningHjemreise(
            fom = fom,
            tom = utledTom(),
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            typeReiseformål = typeReiseformål,
            svar = svar.mapValues { it.value.tilDomene() },
            fakta = fakta.mapTilFakta(),
        )

    private fun utledTom(): LocalDate =
        when (typeReiseformål) {
            TypeReiseformål.HJEMREISE -> tom ?: brukerfeil("tom må settes for hjemreise")
            TypeReiseformål.OPPSTART, TypeReiseformål.AVSLUTNING -> {
                brukerfeilHvis(tom != null) { "tom skal ikke settes for $typeReiseformål" }
                fom
            }
        }
}
