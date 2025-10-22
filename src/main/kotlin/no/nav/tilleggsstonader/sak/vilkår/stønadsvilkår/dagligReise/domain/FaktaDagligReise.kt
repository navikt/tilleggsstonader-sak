package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta

sealed interface FaktaDagligReise {
    val type: TypeDagligReise

    fun mapTilVilkårFakta(): VilkårFakta

    // TODO: Validering
}

data class FaktaOffentligTransport(
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : FaktaDagligReise {
    override val type = TypeDagligReise.OFFENTLIG_TRANSPORT

    override fun mapTilVilkårFakta() =
        FaktaDagligReiseOffentligTransport(
            reisedagerPerUke = reisedagerPerUke,
            prisEnkelbillett = prisEnkelbillett,
            prisSyvdagersbillett = prisSyvdagersbillett,
            prisTrettidagersbillett = prisTrettidagersbillett,
        )
}

data class FaktaPrivatBil(
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: Int,
    val prisBompengerPerDag: Int?,
    val prisFergekostandPerDag: Int?,
) : FaktaDagligReise {
    override val type = TypeDagligReise.PRIVAT_BIL

    override fun mapTilVilkårFakta() =
        FaktaDagligReisePrivatBil(
            reisedagerPerUke = reisedagerPerUke,
            reiseavstandEnVei = reiseavstandEnVei,
            prisBompengerPerDag = prisBompengerPerDag,
            prisFergekostandPerDag = prisFergekostandPerDag,
        )
}
