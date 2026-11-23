package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ROAHReiseVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ROAHVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårGlobalId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.tilVilkårType

object VilkårROAHMapper {
    fun Vilkår.mapTilVilkårROAH(): VilkårROAH =
        when (val fakta = faktaROAHEllerFeil(this.fakta)) {
            is ROAHReiseVilkårFakta -> mapTilReise(fakta)
            is FaktaROAHMidlertidigBosted -> mapTilMidlertidigBosted(fakta)
        }

    private fun Vilkår.mapTilReise(fakta: ROAHReiseVilkårFakta) =
        VilkårReise(
            id = id,
            globalId = globalId,
            behandlingId = behandlingId,
            fom = this.fom ?: error("Forventer at fom er satt"),
            tom = this.tom ?: error("Forventer at tom er satt"),
            aktivitetId = fakta.aktivitetId,
            reiseId = fakta.reiseId,
            typeReiseformål = fakta.typeReiseformål,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett,
            fakta = fakta.mapTilFaktaReiseROAH(),
            parentVilkår = this.parentVilkår ?: error("Reisevilkår $id mangler parentVilkår"),
        )

    private fun Vilkår.mapTilMidlertidigBosted(fakta: FaktaROAHMidlertidigBosted) =
        VilkårMidlertidigBosted(
            id = id,
            globalId = globalId,
            behandlingId = behandlingId,
            fom = this.fom ?: error("Forventer at fom er satt"),
            tom = this.tom ?: error("Forventer at tom er satt"),
            aktivitetId = fakta.aktivitetId,
            resultat = this.resultat,
            status = this.status,
            delvilkårsett = this.delvilkårsett,
            fakta = fakta.mapTilFaktaMidlertidigBosted(),
        )

    private fun faktaROAHEllerFeil(fakta: VilkårFakta?): ROAHVilkårFakta =
        when (fakta) {
            null -> feil("Fakta skal aldri være null for reise oppstart, avslutning og hjemreise")
            is ROAHVilkårFakta -> fakta
            else ->
                feil(
                    "Ugyldig fakta for reise oppstart, avslutning og hjemreise. " +
                        "Forventet ROAHVilkårFakta, fikk ${fakta::class.qualifiedName}",
                )
        }

    fun VilkårROAH.mapTilVilkår(): Vilkår =
        when (this) {
            is VilkårReise -> mapTilVilkår()
            is VilkårMidlertidigBosted -> mapTilVilkår()
        }

    private fun VilkårReise.mapTilVilkår() =
        tilVilkår(
            type = this.typeReiseformål.tilVilkårType(),
            fakta =
                this.fakta.mapTilVilkårFakta(
                    reiseId = this.reiseId,
                    aktivitetId = this.aktivitetId,
                    typeReiseformål = this.typeReiseformål,
                ),
            parentVilkår = this.parentVilkår,
        )

    private fun VilkårMidlertidigBosted.mapTilVilkår() =
        tilVilkår(
            type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
            fakta =
                this.fakta.mapTilVilkårFakta(
                    aktivitetId = this.aktivitetId,
                ),
            parentVilkår = null,
        )

    private fun VilkårROAH.tilVilkår(
        type: VilkårType,
        fakta: VilkårFakta,
        parentVilkår: VilkårGlobalId?,
    ) = Vilkår(
        id = this.id,
        globalId = this.globalId,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        status = this.status,
        type = type,
        fom = this.fom,
        tom = this.tom,
        erFremtidigUtgift = false,
        delvilkårwrapper = DelvilkårWrapper(this.delvilkårsett),
        opphavsvilkår = null,
        gitVersjon = Applikasjonsversjon.versjon,
        fakta = fakta,
        parentVilkår = parentVilkår,
    )

    private fun ROAHReiseVilkårFakta.mapTilFaktaReiseROAH(): FaktaReiseROAH =
        when (this) {
            is FaktaROAHOffentligTransport -> this.mapTilFakta()
            is FaktaROAHPrivatBil -> this.mapTilFakta()
            is FaktaROAHUbestemt -> this.mapTilFakta()
        }

    private fun FaktaROAHOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            nødvendigUtgift = this.nødvendigUtgift,
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaROAHPrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            nødvendigUtgift = this.nødvendigUtgift,
            reiseavstand = this.reiseavstand,
            bompenger = this.bompenger,
            fergekostnad = this.fergekostnad,
            parkering = this.parkering,
            piggdekkavgift = this.piggdekkavgift,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaROAHMidlertidigBosted.mapTilFaktaMidlertidigBosted() =
        FaktaMidlertidigBosted(
            fraAdresse = this.fraAdresse,
            tilAdresse = this.tilAdresse,
        )

    private fun FaktaROAHUbestemt.mapTilFakta() = FaktaUbestemtType
}
