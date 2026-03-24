package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import java.math.BigDecimal
import java.time.LocalDate

data class ReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reiseId: ReiseId,
    val aktivitetsadresse: String?,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val bompengerEnVei: Int?,
    val fergekostandEnVei: Int?,
) : Periode<LocalDate>,
    KopierPeriode<ReiseMedPrivatBil> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): ReiseMedPrivatBil = this.copy(fom = fom, tom = tom)
}

fun VilkårDagligReise.tilReiseMedPrivatBil(): ReiseMedPrivatBil {
    feilHvis(this.fakta !is FaktaPrivatBil) {
        "Forventer kun å få inn vilkår med fakta som er av type privat bil ved beregning av privat bil"
    }

    val fakta = this.fakta
    val periode = fakta.faktaDelperioder.firstOrNull() ?: error("Mangler reiseperiode for privat bil")

    // TODO - her er jeg litt usikker på hva jeg skal gjøre og hvordan vi vil vise rammevedtaket dersom det er flere reiseperioder i perioden med rammevedtak.

    return ReiseMedPrivatBil(
        fom = this.fom,
        tom = this.tom,
        reiseId = fakta.reiseId,
        reisedagerPerUke = periode.reisedagerPerUke,
        reiseavstandEnVei = fakta.reiseavstandEnVei,
        bompengerEnVei = periode.bompengerEnVei,
        fergekostandEnVei = periode.fergekostandEnVei,
        aktivitetsadresse = fakta.adresse,
    )
}
