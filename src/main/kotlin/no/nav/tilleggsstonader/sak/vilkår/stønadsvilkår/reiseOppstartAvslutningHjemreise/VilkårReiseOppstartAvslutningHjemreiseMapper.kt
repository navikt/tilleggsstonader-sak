package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaROAHReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseMidlertidigBosted
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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.tilVilkårType

object VilkårReiseOppstartAvslutningHjemreiseMapper {
    fun Vilkår.mapTilVilkårROAH(): VilkårROAH =
        when (val fakta = faktaROAHEllerFeil(this.fakta)) {
            is FaktaROAHReise -> mapTilReise(fakta)
            is FaktaReiseOppstartAvslutningHjemreiseMidlertidigBosted -> mapTilMidlertidigBosted(fakta)
        }

    private fun Vilkår.mapTilReise(fakta: FaktaROAHReise) =
        VilkårReiseOppstartAvslutningHjemreise(
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
            fakta = fakta.mapTilFaktaReiseOppstartAvslutningHjemreise(),
        )

    private fun Vilkår.mapTilMidlertidigBosted(fakta: FaktaReiseOppstartAvslutningHjemreiseMidlertidigBosted) =
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
            fraAdresse = fakta.fraAdresse,
            tilAdresse = fakta.tilAdresse,
        )

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

    fun VilkårROAH.mapTilVilkår(): Vilkår =
        when (this) {
            is VilkårReiseOppstartAvslutningHjemreise -> mapTilVilkår()
            is VilkårMidlertidigBosted -> mapTilVilkår()
        }

    private fun VilkårReiseOppstartAvslutningHjemreise.mapTilVilkår() =
        tilVilkår(
            type = this.typeReiseformål.tilVilkårType(),
            fakta =
                this.fakta.mapTilVilkårFakta(
                    reiseId = this.reiseId,
                    aktivitetId = this.aktivitetId,
                    typeReiseformål = this.typeReiseformål,
                ),
        )

    private fun VilkårMidlertidigBosted.mapTilVilkår() =
        tilVilkår(
            type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
            fakta =
                FaktaReiseOppstartAvslutningHjemreiseMidlertidigBosted(
                    aktivitetId = this.aktivitetId,
                    fraAdresse = this.fraAdresse,
                    tilAdresse = this.tilAdresse,
                ),
        )

    private fun VilkårROAH.tilVilkår(
        type: VilkårType,
        fakta: VilkårFakta,
    ) = Vilkår(
        id = this.id,
        globalId = this.globalId,
        parentVilkår = null,
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
    )

    private fun FaktaROAHReise.mapTilFaktaReiseOppstartAvslutningHjemreise(): FaktaReiseOppstartAvslutningHjemreise =
        when (this) {
            is FaktaReiseOppstartAvslutningHjemreiseOffentligTransport -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreisePrivatBil -> this.mapTilFakta()
            is FaktaReiseOppstartAvslutningHjemreiseUbestemt -> this.mapTilFakta()
        }

    private fun FaktaReiseOppstartAvslutningHjemreiseOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            nødvendigUtgift = this.nødvendigUtgift,
            utgifterOffentligTransport = this.utgifterOffentligTransport,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaReiseOppstartAvslutningHjemreisePrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            nødvendigUtgift = this.nødvendigUtgift,
            reiseavstand = this.reiseavstand,
            bompenger = this.bompenger,
            fergekostnad = this.fergekostnad,
            parkering = this.parkering,
            piggdekkavgift = this.piggdekkavgift,
            begrunnelse = this.begrunnelse,
        )

    private fun FaktaReiseOppstartAvslutningHjemreiseUbestemt.mapTilFakta() = FaktaUbestemtType
}
