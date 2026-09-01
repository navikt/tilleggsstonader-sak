package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.AktivitetMedReiser
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreisePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårReiseOppstartAvslutningHjemreiseDto

object VilkårReiseOppstartAvslutningHjemreiseDtoMapper {
    fun AktivitetMedReiser.tilDto() =
        AktivitetMedReiserDto(
            aktivitetId = this.aktivitetId,
            aktivitetType = this.aktivitetType,
            tiltaksvariant = this.tiltaksvariant,
            fom = this.fom,
            tom = this.tom,
            reiser = this.reiser.map { it.tilDto() },
        )

    fun VilkårReiseOppstartAvslutningHjemreise.tilDto() =
        VilkårReiseOppstartAvslutningHjemreiseDto(
            id = this.id,
            fom = this.fom,
            tom = this.tom,
            adresse = fakta.adresse,
            reiseId = fakta.reiseId,
            typeReiseformål = fakta.typeReiseformål,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett.map { it.tilDto() },
            fakta = this.fakta.tilDto(),
            slettetKommentar = this.slettetKommentar,
        )

    private fun FaktaReiseOppstartAvslutningHjemreise.tilDto(): FaktaReiseOppstartAvslutningHjemreiseDto =
        when (this) {
            is FaktaOffentligTransport -> this.tilDto()
            is FaktaPrivatBil -> this.tilDto()
            is FaktaUbestemtType -> FaktaReiseOppstartAvslutningHjemreiseUbestemtDto
        }

    private fun FaktaOffentligTransport.tilDto() =
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            aktivitetId = this.aktivitetId,
        )

    private fun FaktaPrivatBil.tilDto() =
        FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
            reiseavstand = this.reiseavstand,
            aktivitetId = this.aktivitetId,
        )
}
