package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingPrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.VilkårReiseTilSamlingDto

object VilkårReiseTilSamlingDtoMapper {
    fun VilkårReiseTilSamling.tilDto() =
        VilkårReiseTilSamlingDto(
            id = this.id,
            fom = this.fom,
            tom = this.tom,
            adresse = fakta.adresse,
            reiseId = fakta.reiseId,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett.map { it.tilDto() },
            fakta = this.fakta.tilDto(),
            slettetKommentar = this.slettetKommentar,
        )

    private fun FaktaReiseTilSamling.tilDto(): FaktaReiseTilSamlingDto =
        when (this) {
            is FaktaOffentligTransport -> this.tilDto()
            is FaktaPrivatBil -> this.tilDto()
            is FaktaUbestemtType -> FaktaReiseTilSamlingUbestemtDto
        }

    private fun FaktaOffentligTransport.tilDto() =
        FaktaReiseTilSamlingOffentligTransportDto(
            utgifterOffentligTransport = this.utgifterOffentligTransport,
        )

    private fun FaktaPrivatBil.tilDto() =
        FaktaReiseTilSamlingPrivatBilDto(
            reiseavstand = this.reiseavstand,
        )
}
