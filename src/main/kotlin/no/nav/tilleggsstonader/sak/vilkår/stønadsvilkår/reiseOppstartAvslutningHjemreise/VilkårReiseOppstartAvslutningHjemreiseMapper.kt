package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise

object VilkårReiseOppstartAvslutningHjemreiseMapper {
    fun Vilkår.mapTilVilkårReiseOppstartAvslutningHjemreise() =
        VilkårReiseOppstartAvslutningHjemreise(
            id = id,
            behandlingId = behandlingId,
            fom = this.fom ?: error("Forventer at fom er satt"),
            tom = this.tom ?: error("Forventer at tom er satt"),
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett,
            fakta = this.fakta.mapTilFaktaReiseOppstartAvslutningHjemreise(),
        )

    fun VilkårReiseOppstartAvslutningHjemreise.mapTilVilkår() =
        Vilkår(
            id = this.id,
            behandlingId = this.behandlingId,
            resultat = this.resultat,
            status = this.status,
            type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
            fom = this.fom,
            tom = this.tom,
            erFremtidigUtgift = false,
            delvilkårwrapper = DelvilkårWrapper(this.delvilkårsett),
            opphavsvilkår = null,
            gitVersjon = Applikasjonsversjon.versjon,
            fakta = this.fakta.mapTilVilkårFakta(),
        )

    private fun VilkårFakta?.mapTilFaktaReiseOppstartAvslutningHjemreise(): FaktaReiseOppstartAvslutningHjemreise =
        when (this) {
            is FaktaReiseOppstartAvslutningHjemreiseOffentligTransport -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreisePrivatBil -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreiseUbestemt -> this.mapTilFakta()
            null -> feil("Fakta skal aldri være null for reise oppstart, avslutning og hjemreise")
            else -> feil("Ugyldig fakta for reise oppstart, avslutning og hjemreise")
        }

    private fun FaktaReiseOppstartAvslutningHjemreiseOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            reiseId = this.reiseId,
            adresse = this.adresse,
            typeReiseformål = this.typeReiseformål,
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            aktivitetId = this.aktivitetId,
        )

    private fun FaktaReiseOppstartAvslutningHjemreisePrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            reiseId = this.reiseId,
            adresse = this.adresse,
            typeReiseformål = this.typeReiseformål,
            reiseavstand = this.reiseavstand,
            aktivitetId = this.aktivitetId,
        )

    private fun FaktaReiseOppstartAvslutningHjemreiseUbestemt.mapTilFakta() =
        FaktaUbestemtType(
            reiseId = this.reiseId,
            adresse = this.adresse,
            typeReiseformål = this.typeReiseformål,
        )
}
