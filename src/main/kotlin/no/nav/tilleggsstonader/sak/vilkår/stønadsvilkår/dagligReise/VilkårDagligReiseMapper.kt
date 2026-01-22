package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType

object VilkårDagligReiseMapper {
    fun Vilkår.mapTilVilkårDagligReise() =
        VilkårDagligReise(
            id = id,
            behandlingId = behandlingId,
            fom = this.fom ?: error("Forventer at fom er satt"),
            tom = this.tom ?: error("Forventer at tom er satt"),
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett,
            fakta = this.fakta.mapTilFaktaDagligReise(),
        )

    fun VilkårDagligReise.mapTilVilkår() =
        Vilkår(
            id = this.id,
            behandlingId = this.behandlingId,
            fom = this.fom,
            tom = this.tom,
            resultat = this.resultat,
            status = this.status,
            type = VilkårType.DAGLIG_REISE,
            delvilkårwrapper = DelvilkårWrapper(this.delvilkårsett),
            fakta = this.fakta.mapTilVilkårFakta(),
            opphavsvilkår = null, // TODO: Ta hensyn til denne i oppdatering
            erFremtidigUtgift = false,
            gitVersjon = Applikasjonsversjon.versjon,
        )

    private fun VilkårFakta?.mapTilFaktaDagligReise(): FaktaDagligReise =
        when (this) {
            is FaktaDagligReiseOffentligTransport -> this.mapTilFakta()
            is FaktaDagligReisePrivatBil -> this.mapTilFakta()
            is FaktaDagligReiseUbestemt -> this.mapTilFakta()
            null -> feil("Fakta skal aldri være null for daglig reise")
        }

    private fun FaktaDagligReiseOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            reiseId = this.reiseId,
            reisedagerPerUke = this.reisedagerPerUke,
            prisEnkelbillett = this.prisEnkelbillett,
            prisSyvdagersbillett = this.prisSyvdagersbillett,
            prisTrettidagersbillett = this.prisTrettidagersbillett,
            adresse = this.adresse,
        )

    private fun FaktaDagligReisePrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            reiseId = this.reiseId,
            reisedagerPerUke = this.reisedagerPerUke,
            reiseavstandEnVei = this.reiseavstandEnVei,
            bompengerEnVei = this.bompengerEnVei,
            fergekostandEnVei = this.fergekostandEnVei,
            adresse = this.adresse,
        )

    private fun FaktaDagligReiseUbestemt.mapTilFakta() =
        FaktaUbestemtType(
            reiseId = this.reiseId,
            adresse = this.adresse,
        )
}
