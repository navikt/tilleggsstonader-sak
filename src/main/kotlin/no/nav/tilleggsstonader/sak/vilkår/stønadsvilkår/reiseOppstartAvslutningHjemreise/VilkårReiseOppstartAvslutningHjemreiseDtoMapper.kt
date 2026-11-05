package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.AktivitetMedReiser
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreisePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårMidlertidigBostedDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårROAHDto
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
            paraplyvilkår = this.paraplyvilkår.map { it.tilDto() },
        )

    fun VilkårROAH.tilDto(): VilkårROAHDto =
        when (this) {
            is VilkårReiseOppstartAvslutningHjemreise -> this.tilDto()
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

    fun VilkårReiseOppstartAvslutningHjemreise.tilDto() =
        VilkårReiseOppstartAvslutningHjemreiseDto(
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

    private fun FaktaReiseOppstartAvslutningHjemreise.tilDto(): FaktaReiseOppstartAvslutningHjemreiseDto =
        when (this) {
            is FaktaOffentligTransport -> this.tilDto()
            is FaktaPrivatBil -> this.tilDto()
            is FaktaUbestemtType -> FaktaReiseOppstartAvslutningHjemreiseUbestemtDto
        }

    private fun FaktaOffentligTransport.tilDto() =
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
            nødvendigUtgift = nødvendigUtgift.tilDto(),
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaPrivatBil.tilDto() =
        FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
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
