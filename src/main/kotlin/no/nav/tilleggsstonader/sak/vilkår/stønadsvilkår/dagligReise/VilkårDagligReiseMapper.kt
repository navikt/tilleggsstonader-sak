package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
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
            fakta = this.fakta?.mapTilFaktaDagligReise(type = this.fakta.typeVilkårFakta),
        )

    fun VilkårDagligReise.mapTilVilkår() =
        Vilkår(
            id = this.id,
            behandlingId = this.behandlingId,
            fom = this.fom,
            tom = this.tom,
            resultat = this.resultat,
            status = this.status,
            type = VilkårType.DAGLIG_REISE_OFFENTLIG_TRANSPORT,
            delvilkårwrapper = DelvilkårWrapper(this.delvilkårsett),
            fakta = this.fakta?.mapTilVilkårFakta(),
            opphavsvilkår = null, // TODO: Ta hensyn til denne i oppdatering
            erFremtidigUtgift = false,
            offentligTransport = null,
            gitVersjon = Applikasjonsversjon.versjon,
        )

    private fun VilkårFakta.mapTilFaktaDagligReise(type: TypeVilkårFakta) =
        when (this) {
            is FaktaDagligReiseOffentligTransport -> this.mapTilFakta()
            is FaktaDagligReisePrivatBil -> this.mapTilFakta()
            else -> error("Fakta av type=$type finnes ikke for vilkår på daglig reise")
        }

    private fun FaktaDagligReiseOffentligTransport.mapTilFakta() =
        FaktaOffentligTransport(
            reisedagerPerUke = this.reisedagerPerUke,
            prisEnkelbillett = this.prisEnkelbillett,
            prisSyvdagersbillett = this.prisSyvdagersbillett,
            prisTrettidagersbillett = this.prisTrettidagersbillett,
        )

    private fun FaktaDagligReisePrivatBil.mapTilFakta() =
        FaktaPrivatBil(
            reisedagerPerUke = this.reisedagerPerUke,
            reiseavstandEnVei = this.reiseavstandEnVei,
            prisBompengerPerDag = this.prisBompengerPerDag,
            prisFergekostandPerDag = this.prisFergekostandPerDag,
        )

    fun TypeVilkårFakta.tilTypeDagligReise() =
        when (this) {
            TypeVilkårFakta.DAGLIG_REISE_OFFENTLIG_TRANSPORT -> TypeDagligReise.OFFENTLIG_TRANSPORT
            TypeVilkårFakta.DAGLIG_REISE_PRIVAT_BIL -> TypeDagligReise.PRIVAT_BIL

            else -> error("Faktatype $this tilhører ikke daglig reise")
        }
}
