package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta

sealed interface FaktaDagligReise {
    val type: TypeDagligReise

    fun mapTilVilkårFakta(): VilkårFakta
}

data class FaktaOffentligTransport(
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : FaktaDagligReise {
    override val type = TypeDagligReise.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
        validerMinstEnBillettPris()
        validerReisdager()
    }

    private fun validerIngenNegativeUtgifter() {
        brukerfeilHvis(
            (prisEnkelbillett != null && prisEnkelbillett <= 0) ||
                (prisSyvdagersbillett != null && prisSyvdagersbillett <= 0) ||
                (prisTrettidagersbillett != null && prisTrettidagersbillett <= 0),
        ) {
            "Billettprisen må være større enn 0"
        }
    }

    private fun validerMinstEnBillettPris() {
        brukerfeilHvis(prisEnkelbillett == null && prisSyvdagersbillett == null && prisTrettidagersbillett == null) {
            "Minst en billettpris må være satt"
        }
    }

    private fun validerReisdager() {
        brukerfeilHvis(reisedagerPerUke < 0) {
            "Reisedager per uke må være 0 eller mer"
        }

        brukerfeilHvis(reisedagerPerUke > 5) {
            "Reisedager per uke kan ikke være mer enn 5"
        }
    }

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

    init {
        validerIngenNegativeUtgifter()
        validerReisdager()
        validerIngenNegativReiseavstand()
    }

    private fun validerIngenNegativeUtgifter() {
        brukerfeilHvis(
            (prisBompengerPerDag != null && prisBompengerPerDag < 0) ||
                (prisFergekostandPerDag != null && prisFergekostandPerDag < 0),
        ) {
            "Bompenge- og fergeprisen må være større enn 0"
        }
    }

    private fun validerIngenNegativReiseavstand() {
        brukerfeilHvis(reiseavstandEnVei < 0) {
            "Reiseavstanden må være større enn 0"
        }
    }

    private fun validerReisdager() {
        brukerfeilHvis(reisedagerPerUke < 0) {
            "Reisedager per uke må være 0 eller mer"
        }

        brukerfeilHvis(reisedagerPerUke > 7) {
            "Reisedager per uke kan ikke være mer enn 7"
        }
    }

    override fun mapTilVilkårFakta() =
        FaktaDagligReisePrivatBil(
            reisedagerPerUke = reisedagerPerUke,
            reiseavstandEnVei = reiseavstandEnVei,
            prisBompengerPerDag = prisBompengerPerDag,
            prisFergekostandPerDag = prisFergekostandPerDag,
        )
}
