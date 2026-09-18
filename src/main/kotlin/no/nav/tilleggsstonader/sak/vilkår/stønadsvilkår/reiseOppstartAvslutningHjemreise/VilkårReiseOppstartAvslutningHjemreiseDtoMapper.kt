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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.AktivitetPåFaktaDto

object VilkårReiseOppstartAvslutningHjemreiseDtoMapper {
    fun AktivitetMedReiser.tilDto() =
        AktivitetMedReiserDto(
            aktivitetId = this.aktivitetId,
            aktivitetType = this.aktivitetType,
            tiltaksvariant = this.tiltaksvariant,
            fom = this.fom,
            tom = this.tom,
            reiser =
                this.reiser.map {
                    it.tilDto(
                        aktivitet =
                            AktivitetPåFaktaDto(
                                aktivitetType = this.aktivitetType.name,
                                tiltaksvariant = this.tiltaksvariant?.beskrivelse,
                                fom = this.fom,
                                tom = this.tom,
                            ),
                    )
                },
        )

    fun VilkårReiseOppstartAvslutningHjemreise.tilDto(aktivitet: AktivitetPåFaktaDto? = null) =
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
            fakta = this.fakta.tilDto(aktivitet = aktivitet),
            slettetKommentar = this.slettetKommentar,
        )

    private fun FaktaReiseOppstartAvslutningHjemreise.tilDto(aktivitet: AktivitetPåFaktaDto?): FaktaReiseOppstartAvslutningHjemreiseDto =
        when (this) {
            is FaktaOffentligTransport -> this.tilDto(aktivitet = aktivitet)
            is FaktaPrivatBil -> this.tilDto(aktivitet = aktivitet)
            is FaktaUbestemtType -> FaktaReiseOppstartAvslutningHjemreiseUbestemtDto
        }

    private fun FaktaOffentligTransport.tilDto(aktivitet: AktivitetPåFaktaDto?) =
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            aktivitetId = this.aktivitetId,
            aktivitetType = aktivitet?.aktivitetType,
            aktivitet = aktivitet,
        )

    private fun FaktaPrivatBil.tilDto(aktivitet: AktivitetPåFaktaDto?) =
        FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
            reiseavstand = this.reiseavstand,
            aktivitetId = this.aktivitetId,
            aktivitetType = aktivitet?.aktivitetType,
            aktivitet = aktivitet,
            bompenger = this.bompenger,
            fergekostnad = this.fergekostnad,
        )
}
