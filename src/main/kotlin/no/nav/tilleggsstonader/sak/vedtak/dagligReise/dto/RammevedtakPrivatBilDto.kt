package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class RammevedtakPrivatBilDto(
    val reiser: List<RammeForReiseMedPrivatBilDto>,
)

data class RammeForReiseMedPrivatBilDto(
    val reiseId: ReiseId,
    val fom: LocalDate,
    val tom: LocalDate,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val bompengerEnVei: Int?,
    val fergekostnadEnVei: Int?,
    val aktivitetsadresse: String?,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal,
    val uker: List<RammeForUkeDto>,
)

data class RammeForUkeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val maksAntallDagerSomKanDekkes: Int,
    val antallDagerInkludererHelg: Boolean,
    val maksBeløpSomKanDekkesFørParkering: BigInteger,
)

fun RammevedtakPrivatBil.tilDto() =
    RammevedtakPrivatBilDto(
        reiser = reiser.flatMap { it.tilDto() },
    )

// TODO: Flytt splitting til beregning dersom vi vil beholde det
private fun RammeForReiseMedPrivatBil.tilDto(): List<RammeForReiseMedPrivatBilDto> {
    val ukerGruppertPåSats = this.uker.groupBy { it.grunnlag.kilometersats }

    return ukerGruppertPåSats.entries.map { (kilometersats, ukerMedSammeSats) ->
        RammeForReiseMedPrivatBilDto(
            reiseId = reiseId,
            fom = ukerMedSammeSats.first().grunnlag.fom,
            tom = ukerMedSammeSats.last().grunnlag.tom,
            reisedagerPerUke = grunnlag.reisedagerPerUke,
            reiseavstandEnVei = grunnlag.reiseavstandEnVei,
            bompengerEnVei = grunnlag.ekstrakostnader.bompengerEnVei,
            kilometersats = kilometersats,
            dagsatsUtenParkering = ukerMedSammeSats.first().dagsatsUtenParkering,
            fergekostnadEnVei = grunnlag.ekstrakostnader.fergekostnadEnVei,
            uker = ukerMedSammeSats.map { it.tilDto() },
            aktivitetsadresse = this.aktivitetsadresse,
        )
    }
}

private fun RammeForUke.tilDto() =
    RammeForUkeDto(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        maksAntallDagerSomKanDekkes = grunnlag.maksAntallDagerSomKanDekkes,
        antallDagerInkludererHelg = grunnlag.antallDagerInkludererHelg,
        maksBeløpSomKanDekkesFørParkering = maksBeløpSomKanDekkesFørParkering,
    )
