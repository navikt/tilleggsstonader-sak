package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHMidlertidigBostedPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.NødvendigUtgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.AktivitetMedReiser
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaMidlertidigBostedPeriode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaMidlertidigBostedPeriodeDto
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
            midlertidigeBostedsperioder = this.midlertidigeBostedsperioder.map { it.tilDto() },
        )

    fun Vilkår.tilDto(): VilkårReiseOppstartAvslutningHjemreiseDto {
        val fakta = fakta
        if (fakta !is FaktaROAHMidlertidigBostedPeriode) return mapTilVilkårReiseOppstartAvslutningHjemreise().tilDto()
        return VilkårReiseOppstartAvslutningHjemreiseDto(
            id = id,
            fom = requireNotNull(fom),
            tom = requireNotNull(tom),
            resultat = resultat,
            status = status,
            delvilkårsett = delvilkårsett.map { it.tilDto() },
            fakta = FaktaMidlertidigBostedPeriodeDto(fakta.aktivitetId, fakta.midlertidigAdresse, fakta.hjemstedAdresse),
            slettetKommentar = slettetKommentar,
        )
    }

    fun VilkårReiseOppstartAvslutningHjemreise.tilDto() =
        VilkårReiseOppstartAvslutningHjemreiseDto(
            id = this.id,
            fom = this.fom,
            tom = this.tom,
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
            is FaktaUbestemtType -> FaktaReiseOppstartAvslutningHjemreiseUbestemtDto(reiseId, aktivitetId, typeReiseformål)
            is FaktaMidlertidigBostedPeriode -> FaktaMidlertidigBostedPeriodeDto(aktivitetId, midlertidigAdresse, hjemstedAdresse)
        }

    private fun FaktaOffentligTransport.tilDto() =
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            typeReiseformål = typeReiseformål,
            nødvendigUtgift = nødvendigUtgift.tilDto(),
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaPrivatBil.tilDto() =
        FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            typeReiseformål = typeReiseformål,
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
