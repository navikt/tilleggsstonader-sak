package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReisePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto

object VilkårDagligReiseDtoMapper {
    fun VilkårDagligReise.tilDto() =
        VilkårDagligReiseDto(
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

    private fun FaktaDagligReise.tilDto(): FaktaDagligReiseDto =
        when (this) {
            is FaktaOffentligTransport -> this.tilDto()
            is FaktaPrivatBil -> this.tilDto()
            is FaktaUbestemtType -> FaktaDagligReiseUbestemtDto
        }

    private fun FaktaOffentligTransport.tilDto() =
        FaktaDagligReiseOffentligTransportDto(
            reisedagerPerUke = reisedagerPerUke,
            prisEnkelbillett = prisEnkelbillett,
            prisSyvdagersbillett = prisSyvdagersbillett,
            prisTrettidagersbillett = prisTrettidagersbillett,
        )

    private fun FaktaPrivatBil.tilDto() =
        FaktaDagligReisePrivatBilDto(
            reisedagerPerUke = reisedagerPerUke,
            reiseavstandEnVei = reiseavstandEnVei,
            bompengerEnVei = bompengerEnVei,
            fergekostandEnVei = fergekostandEnVei,
        )
}
