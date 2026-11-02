package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.ReiseFaktaROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise

object VilkårReiseOppstartAvslutningHjemreiseMapper {
    fun Vilkår.mapTilVilkårReiseOppstartAvslutningHjemreise(): VilkårReiseOppstartAvslutningHjemreise {
        val fakta = faktaROAHEllerFeil(this.fakta)
        return VilkårReiseOppstartAvslutningHjemreise(
            id = id,
            behandlingId = behandlingId,
            fom = this.fom ?: error("Forventer at fom er satt"),
            tom = this.tom ?: error("Forventer at tom er satt"),
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett,
            fakta = fakta.mapTilFaktaReiseOppstartAvslutningHjemreise(),
            slettetKommentar = slettetKommentar,
        )
    }

    private fun faktaROAHEllerFeil(fakta: VilkårFakta?): FaktaROAHReise =
        when (fakta) {
            null -> feil("Fakta skal aldri være null for reise oppstart, avslutning og hjemreise")
            is FaktaROAHReise -> fakta
            else ->
                feil(
                    "Ugyldig fakta for reise oppstart, avslutning og hjemreise. " +
                        "Forventet FaktaROAH, fikk ${fakta::class.qualifiedName}",
                )
        }

    private fun FaktaROAHReise.mapTilFaktaReiseOppstartAvslutningHjemreise(): ReiseFaktaROAH =
        when (this) {
            is FaktaReiseOppstartAvslutningHjemreiseOffentligTransport -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreisePrivatBil -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreiseUbestemt -> this.mapTilFakta()
        }

    private fun FaktaReiseOppstartAvslutningHjemreiseOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            typeReiseformål = typeReiseformål,
            nødvendigUtgift = this.nødvendigUtgift,
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaReiseOppstartAvslutningHjemreisePrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            reiseId = reiseId,
            aktivitetId = aktivitetId,
            typeReiseformål = typeReiseformål,
            nødvendigUtgift = this.nødvendigUtgift,
            reiseavstand = this.reiseavstand,
            bompenger = this.bompenger,
            fergekostnad = this.fergekostnad,
            parkering = this.parkering,
            piggdekkavgift = this.piggdekkavgift,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaReiseOppstartAvslutningHjemreiseUbestemt.mapTilFakta() = FaktaUbestemtType(reiseId, aktivitetId, typeReiseformål)
}
