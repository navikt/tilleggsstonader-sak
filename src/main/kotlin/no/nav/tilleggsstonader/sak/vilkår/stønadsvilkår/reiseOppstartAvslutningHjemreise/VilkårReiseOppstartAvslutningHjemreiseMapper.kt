package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAH
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
    fun Vilkår.mapTilVilkårReiseOppstartAvslutningHjemreise(): VilkårReiseOppstartAvslutningHjemreise {
        val fakta = faktaROAHEllerFeil(this.fakta)
        return VilkårReiseOppstartAvslutningHjemreise(
            id = id,
            behandlingId = behandlingId,
            fom = this.fom ?: error("Forventer at fom er satt"),
            tom = this.tom ?: error("Forventer at tom er satt"),
            aktivitetId = fakta.aktivitetId,
            reiseId = fakta.reiseId,
            typeReiseformål = fakta.typeReiseformål,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett,
            fakta = fakta.mapTilFaktaReiseOppstartAvslutningHjemreise(),
        )
    }

    private fun faktaROAHEllerFeil(fakta: VilkårFakta?): FaktaROAH =
        when (fakta) {
            null -> feil("Fakta skal aldri være null for reise oppstart, avslutning og hjemreise")
            is FaktaROAH -> fakta
            else ->
                feil(
                    "Ugyldig fakta for reise oppstart, avslutning og hjemreise. " +
                        "Forventet FaktaROAH, fikk ${fakta::class.qualifiedName}",
                )
        }

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
            fakta =
                this.fakta.mapTilVilkårFakta(
                    reiseId = this.reiseId,
                    aktivitetId = this.aktivitetId,
                    typeReiseformål = this.typeReiseformål,
                ),
        )

    private fun FaktaROAH.mapTilFaktaReiseOppstartAvslutningHjemreise(): FaktaReiseOppstartAvslutningHjemreise =
        when (this) {
            is FaktaReiseOppstartAvslutningHjemreiseOffentligTransport -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreisePrivatBil -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreiseUbestemt -> this.mapTilFakta()
        }

    private fun FaktaReiseOppstartAvslutningHjemreiseOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaReiseOppstartAvslutningHjemreisePrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            reiseavstand = this.reiseavstand,
            bompenger = this.bompenger,
            fergekostnad = this.fergekostnad,
            parkering = this.parkering,
            piggdekkavgift = this.piggdekkavgift,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaReiseOppstartAvslutningHjemreiseUbestemt.mapTilFakta() = FaktaUbestemtType
}
