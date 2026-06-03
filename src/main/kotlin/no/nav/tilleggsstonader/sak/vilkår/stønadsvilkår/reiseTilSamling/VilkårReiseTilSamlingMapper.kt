package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling

object VilkårReiseTilSamlingMapper {
    fun Vilkår.mapTilVilkårReiseTilSamling() =
        VilkårReiseTilSamling(
            id = id,
            behandlingId = behandlingId,
            fom = this.fom ?: error("Forventer at fom er satt"),
            tom = this.tom ?: error("Forventer at tom er satt"),
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett,
            fakta = this.fakta.mapTilFaktaReiseTilSamling(),
        )

    fun VilkårReiseTilSamling.mapTilVilkår() =
        Vilkår(
            id = this.id,
            behandlingId = this.behandlingId,
            resultat = this.resultat,
            status = this.status,
            type = VilkårType.REISE_TIL_SAMLING,
            fom = this.fom,
            tom = this.tom,
            erFremtidigUtgift = false,
            delvilkårwrapper = DelvilkårWrapper(this.delvilkårsett),
            opphavsvilkår = null,
            gitVersjon = Applikasjonsversjon.versjon,
            fakta = this.fakta.mapTilVilkårFakta(),
        )

    private fun VilkårFakta?.mapTilFaktaReiseTilSamling(): FaktaReiseTilSamling =
        when (this) {
            is FaktaReiseTilSamlingOffentligTransport -> this.mapTilFakta()
            is FaktaReiseTilSamlingPrivatBil -> this.mapTilFakta()
            is FaktaReiseTilSamlingUbestemt -> this.mapTilFakta()
            null -> feil("Fakta skal aldri være null for reise til samling")
            else -> feil("Ugyldig fakta for reise til samling")
        }

    private fun FaktaReiseTilSamlingOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            reiseId = this.reiseId,
            adresse = this.adresse,
            utgifterOffentligTransport = this.utgifterOffentligTransport,
        )

    private fun FaktaReiseTilSamlingPrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            reiseId = this.reiseId,
            adresse = this.adresse,
            reiseavstand = this.reiseavstand,
        )

    private fun FaktaReiseTilSamlingUbestemt.mapTilFakta() =
        FaktaUbestemtType(
            reiseId = this.reiseId,
            adresse = this.adresse,
        )
}
