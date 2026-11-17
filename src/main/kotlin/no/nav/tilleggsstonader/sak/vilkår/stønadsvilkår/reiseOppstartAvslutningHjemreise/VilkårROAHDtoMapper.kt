package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.AktivitetMedReiser
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaROAHOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaROAHPrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaROAHUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårMidlertidigBostedDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårROAHReiseDto

object VilkårROAHDtoMapper {
    fun AktivitetMedReiser.tilDto() =
        AktivitetMedReiserDto(
            aktivitetId = this.aktivitetId,
            aktivitetType = this.aktivitetType,
            tiltaksvariant = this.tiltaksvariant,
            fom = this.fom,
            tom = this.tom,
            reiser = this.reiser.map { it.tilDto() },
            paraplyvilkår = this.paraplyvilkår.map { it.tilDto() },
        )

    fun VilkårROAH.tilDto(): VilkårROAHDto =
        when (this) {
            is VilkårReise -> this.tilDto()
            is VilkårMidlertidigBosted -> this.tilDto()
        }

    fun VilkårMidlertidigBosted.tilDto() =
        VilkårMidlertidigBostedDto(
            id = this.id,
            globalId = this.globalId,
            fom = this.fom,
            tom = this.tom,
            aktivitetId = this.aktivitetId,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett.map { it.tilDto() },
            fraAdresse = this.fraAdresse,
            tilAdresse = this.tilAdresse,
            slettetKommentar = this.slettetKommentar,
        )

    fun VilkårReise.tilDto() =
        VilkårROAHReiseDto(
            id = this.id,
            globalId = this.globalId,
            fom = this.fom,
            tom = this.tom,
            reiseId = this.reiseId,
            aktivitetId = this.aktivitetId,
            typeReiseformål = this.typeReiseformål,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett.map { it.tilDto() },
            fakta = this.fakta.tilDto(),
            slettetKommentar = this.slettetKommentar,
            parentVilkår = this.parentVilkår,
        )

    private fun FaktaROAH.tilDto(): FaktaROAHDto =
        when (this) {
            is FaktaOffentligTransport -> this.tilDto()
            is FaktaPrivatBil -> this.tilDto()
            is FaktaUbestemtType -> FaktaROAHUbestemtDto
        }

    private fun FaktaOffentligTransport.tilDto() =
        FaktaROAHOffentligTransportDto(
            nødvendigUtgift = nødvendigUtgift.tilDto(),
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaPrivatBil.tilDto() =
        FaktaROAHPrivatBilDto(
            nødvendigUtgift = nødvendigUtgift.tilDto(),
            reiseavstand = this.reiseavstand,
            bompenger = this.bompenger,
            fergekostnad = this.fergekostnad,
            parkering = this.parkering,
            piggdekkavgift = this.piggdekkavgift,
            begrunnelse = this.begrunnelse,
        )

    private fun NødvendigUtgift.tilDto() =
        NødvendigUtgiftDto(
            erUtgiftenNødvendig = this.erUtgiftenNødvendig,
            begrunnelse = this.begrunnelse,
        )
}
