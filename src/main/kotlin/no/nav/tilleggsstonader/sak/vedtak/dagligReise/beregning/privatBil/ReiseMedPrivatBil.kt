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
    val delPerioder: List<Delperiode>,
    val reiseavstandEnVei: BigDecimal,
) : Periode<LocalDate>,
    KopierPeriode<ReiseMedPrivatBil> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): ReiseMedPrivatBil = this.copy(fom = fom, tom = tom)
}

data class Delperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
) : Periode<LocalDate>,
    KopierPeriode<Delperiode> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ) = Delperiode(
        fom = fom,
        tom = tom,
        reisedagerPerUke = reisedagerPerUke,
        bompengerPerDag = bompengerPerDag,
        fergekostnadPerDag = fergekostnadPerDag,
    )
}

fun VilkårDagligReise.tilReiserMedPrivatBil(): ReiseMedPrivatBil {
    feilHvis(this.fakta !is FaktaPrivatBil) {
        "Forventer kun å få inn vilkår med fakta som er av type privat bil ved beregning av privat bil"
    }

    return ReiseMedPrivatBil(
        fom = this.fom,
        tom = this.tom,
        aktivitetsadresse = this.fakta.adresse,
        reiseId = this.fakta.reiseId,
        reiseavstandEnVei = fakta.reiseavstandEnVei,
        delPerioder =
            fakta.faktaDelperioder.map { delperiode ->
                Delperiode(
                    fom = delperiode.fom,
                    tom = delperiode.tom,
                    reisedagerPerUke = delperiode.reisedagerPerUke,
                    bompengerPerDag = delperiode.bompengerPerDag,
                    fergekostnadPerDag = delperiode.fergekostnadPerDag,
                )
            },
    )
}
