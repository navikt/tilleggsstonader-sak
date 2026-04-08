package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.math.BigDecimal
import java.time.LocalDate

data class ReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reiseId: ReiseId,
    val aktivitetsadresse: String?,
    val aktivitetType: AktivitetType,
    val typeAktivitet: TypeAktivitet?,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
) : Periode<LocalDate>,
    KopierPeriode<ReiseMedPrivatBil> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): ReiseMedPrivatBil = this.copy(fom = fom, tom = tom)
}

fun VilkårDagligReise.tilReiseMedPrivatBil(
    aktivitetType: AktivitetType,
    typeAktivitet: TypeAktivitet?,
): ReiseMedPrivatBil {
    feilHvis(this.fakta !is FaktaPrivatBil) {
        "Forventer kun å få inn vilkår med fakta som er av type privat bil ved beregning av privat bil"
    }

    val fakta = this.fakta
    val periode = fakta.faktaDelperioder.single()

    // TODO - her er jeg litt usikker på hva jeg skal gjøre og hvordan vi vil vise rammevedtaket dersom det er flere reiseperioder i perioden med rammevedtak.

    return ReiseMedPrivatBil(
        fom = this.fom,
        tom = this.tom,
        reiseId = fakta.reiseId,
        reisedagerPerUke = periode.reisedagerPerUke,
        reiseavstandEnVei = fakta.reiseavstandEnVei,
        bompengerPerDag = periode.bompengerPerDag,
        fergekostnadPerDag = periode.fergekostnadPerDag,
        aktivitetsadresse = fakta.adresse,
        aktivitetType = aktivitetType,
        typeAktivitet = typeAktivitet,
    )
}
