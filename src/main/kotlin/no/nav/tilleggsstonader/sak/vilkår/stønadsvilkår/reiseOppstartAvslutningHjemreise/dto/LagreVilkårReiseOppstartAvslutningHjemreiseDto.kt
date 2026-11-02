package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.libs.feil.brukerfeil
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.LagreVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import java.time.LocalDate

data class LagreVilkårReiseOppstartAvslutningHjemreiseDto(
    val fom: LocalDate,
    val tom: LocalDate?,
    val svar: Map<RegelId, SvarOgBegrunnelseDto>,
    val fakta: FaktaReiseOppstartAvslutningHjemreiseDto,
) : LagreVilkår {
    fun tilDomene() =
        LagreVilkårReiseOppstartAvslutningHjemreise(
            fom = fom,
            tom = utledTom(),
            svar = svar.mapValues { it.value.tilDomene() },
            fakta = fakta.mapTilFakta(),
        )

    private fun utledTom(): LocalDate =
        when (val fakta = fakta) {
            is FaktaMidlertidigBostedPeriodeDto -> tom ?: brukerfeil("tom må settes for midlertidig bostedsperiode")
            is ReiseFaktaROAHDto ->
                when (val typeReiseformål = fakta.typeReiseformål) {
                    TypeReiseformål.HJEMREISE -> tom ?: brukerfeil("tom må settes for hjemreise")
                    TypeReiseformål.OPPSTART, TypeReiseformål.AVSLUTNING -> {
                        brukerfeilHvis(tom != null && tom != fom) { "Tom kan ikke settes til noe annet enn lik fom for $typeReiseformål" }
                        fom
                    }
                }
        }
}
