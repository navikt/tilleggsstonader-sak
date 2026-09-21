package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.AktivitetInfoDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingPrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.VilkårReiseTilSamlingDto

object VilkårReiseTilSamlingDtoMapper {
    fun VilkårReiseTilSamling.tilDto(aktivitet: AktivitetInfoDto? = null) =
        VilkårReiseTilSamlingDto(
            id = this.id,
            fom = this.fom,
            tom = this.tom,
            adresse = fakta.adresse,
            reiseId = fakta.reiseId,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett.map { it.tilDto() },
            fakta = this.fakta.tilDto(aktivitet = aktivitet),
            slettetKommentar = this.slettetKommentar,
        )

    private fun FaktaReiseTilSamling.tilDto(aktivitet: AktivitetInfoDto?): FaktaReiseTilSamlingDto =
        when (this) {
            is FaktaOffentligTransport -> this.tilDto(aktivitet = aktivitet)
            is FaktaPrivatBil -> this.tilDto(aktivitet = aktivitet)
            is FaktaUbestemtType -> FaktaReiseTilSamlingUbestemtDto
        }

    private fun FaktaOffentligTransport.tilDto(aktivitet: AktivitetInfoDto?) =
        FaktaReiseTilSamlingOffentligTransportDto(
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
            aktivitet = aktivitet,
        )

    private fun FaktaPrivatBil.tilDto(aktivitet: AktivitetInfoDto?) =
        FaktaReiseTilSamlingPrivatBilDto(
            reiseavstand = this.reiseavstand,
            begrunnelse = this.begrunnelse,
            aktivitet = aktivitet,
            bompenger = this.bompenger,
            fergekostnad = this.fergekostnad,
            parkering = this.parkering,
            piggdekkavgift = this.piggdekkavgift,
        )
}
