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
    val delPerioder: List<ReiseMedPrivatBilDelperiode>,
    val reiseavstandEnVei: BigDecimal,
) : Periode<LocalDate>,
    KopierPeriode<ReiseMedPrivatBil> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): ReiseMedPrivatBil = this.copy(fom = fom, tom = tom)
}

data class ReiseMedPrivatBilDelperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
) : Periode<LocalDate>,
    KopierPeriode<ReiseMedPrivatBilDelperiode> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ) = ReiseMedPrivatBilDelperiode(
        fom = fom,
        tom = tom,
        reisedagerPerUke = reisedagerPerUke,
        bompengerPerDag = bompengerPerDag,
        fergekostnadPerDag = fergekostnadPerDag,
    )
}

fun VilkårDagligReise.tilReiserMedPrivatBil(
    aktivitetType: AktivitetType,
    typeAktivitet: TypeAktivitet?,
    gjelderTiltaksenheten: Boolean
): ReiseMedPrivatBil {
    feilHvis(this.fakta !is FaktaPrivatBil) {
        "Forventer kun å få inn vilkår med fakta som er av type privat bil ved beregning av privat bil"
    }

    feilHvis(gjelderTiltaksenheten && typeAktivitet == null){
        "Foventer at typeAktivitet ikke er null når oppretter reise med privat bil for tiltaksenheten"
    }

    return ReiseMedPrivatBil(
        fom = this.fom,
        tom = this.tom,
        aktivitetsadresse = this.fakta.adresse,
        reiseId = this.fakta.reiseId,
        reiseavstandEnVei = fakta.reiseavstandEnVei,
        delPerioder =
            fakta.faktaDelperioder.map { delperiode ->
                ReiseMedPrivatBilDelperiode(
                    fom = delperiode.fom,
                    tom = delperiode.tom,
                    reisedagerPerUke = delperiode.reisedagerPerUke,
                    bompengerPerDag = delperiode.bompengerPerDag,
                    fergekostnadPerDag = delperiode.fergekostnadPerDag,
                )
            },
        aktivitetType = aktivitetType,
        typeAktivitet = typeAktivitet,
    )
}
