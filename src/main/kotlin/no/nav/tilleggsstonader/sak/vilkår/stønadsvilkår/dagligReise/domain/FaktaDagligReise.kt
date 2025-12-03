package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.antallDagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFakta

sealed interface FaktaDagligReise {
    val type: TypeDagligReise

    fun mapTilVilkårFakta(): VilkårFakta
}

data class FaktaOffentligTransport(
    val reiseId: ReiseId? = ReiseId.random(), // TODO: Fjern nullbarhet
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
    val periode: Datoperiode? = null,
) : FaktaDagligReise {
    override val type = TypeDagligReise.OFFENTLIG_TRANSPORT

    init {
        validerIngenNegativeUtgifter()
        validerMinstEnBillettPris()
        validerReisdager()
        validerBillettpriser()
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

    private fun validerBillettpriser() {
        if (periode == null) {
            return
        }

        val dager = antallDagerIPeriodeInklusiv(periode.fom, periode.tom)
        val manglerEnkelbillett = prisEnkelbillett == null
        val manglerTrettidagersbillett = prisTrettidagersbillett == null
        val eksaktTrettidagersperiode = dager % 30 == 0
        val mindreEnnTrettiDager = dager < 30
        val overTrettiDager = dager >= 30
        val reiserOfte = reisedagerPerUke >= 3
        val reiserSjeldent = reisedagerPerUke < 3
        // Vi regner om perioden til uker og ganger med antall reisedager for å få summen av reisedagene i perioden
        val faktiskeReisedager = dager / 7 * reisedagerPerUke
        // Vi bruker 9 reisedager som grense for når månedskort anbefales, da det vil være rimeligere enn enkeltbilletter
        val grenseHvorMånedskortLønnerSeg = 9

        brukerfeilHvis((reiserSjeldent || faktiskeReisedager < grenseHvorMånedskortLønnerSeg) && manglerEnkelbillett) {
            "Pris for enkeltbillett må fylles ut når det reises sjeldent eller over en kort periode"
        }

        brukerfeilHvis(faktiskeReisedager >= grenseHvorMånedskortLønnerSeg && mindreEnnTrettiDager && manglerTrettidagersbillett) {
            "Pris for 30-dagersbillett må fylles ut da det lønner seg med 30-dagersbillett for denne perioden"
        }

        brukerfeilHvis(overTrettiDager && reiserOfte && manglerTrettidagersbillett) {
            "Pris for 30-dagersbillett må fylles ut når det reises regelmessig over lengre tid"
        }

        brukerfeilHvis(eksaktTrettidagersperiode && reiserOfte && manglerTrettidagersbillett) {
            "Pris for 30-dagersbillett må fylles ut da reisen går opp i eksakte trettidagersperioder"
        }

        brukerfeilHvis(!eksaktTrettidagersperiode && overTrettiDager && manglerEnkelbillett) {
            "Pris for enkeltbillett må fylles ut siden reisen varer lenger enn 30 dager uten å være en eksakt 30-dagersperiode"
        }
    }

    override fun mapTilVilkårFakta() =
        FaktaDagligReiseOffentligTransport(
            reiseId = reiseId,
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
