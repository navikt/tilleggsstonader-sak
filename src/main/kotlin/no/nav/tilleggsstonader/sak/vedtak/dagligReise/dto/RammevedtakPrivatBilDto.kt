package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
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
    val uker: List<RammeForUkeDto>,
)

data class RammeForUkeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val maksAntallDagerSomKanDekkes: Int,
    val antallDagerInkludererHelg: Boolean,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal,
    val maksBeløpSomKanDekkesFørParkering: BigInteger,
)

fun RammevedtakPrivatBil.tilDto() =
    RammevedtakPrivatBilDto(
        reiser = reiser.map { it.tilDto() },
    )

private fun RammeForReiseMedPrivatBil.tilDto() =
    RammeForReiseMedPrivatBilDto(
        reiseId = reiseId,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        reisedagerPerUke = grunnlag.reisedagerPerUke,
        reiseavstandEnVei = grunnlag.reiseavstandEnVei,
        bompengerEnVei = grunnlag.ekstrakostnader.bompengerEnVei,
        fergekostnadEnVei = grunnlag.ekstrakostnader.fergekostnadEnVei,
        uker = uker.map { it.tilDto() },
    )

private fun RammeForUke.tilDto() =
    RammeForUkeDto(
        fom = grunnlag.fom,
        tom = grunnlag.fom,
        maksAntallDagerSomKanDekkes = grunnlag.maksAntallDagerSomKanDekkes,
        antallDagerInkludererHelg = grunnlag.antallDagerInkludererHelg,
        kilometersats = grunnlag.kilometersats,
        dagsatsUtenParkering = dagsatsUtenParkering,
        maksBeløpSomKanDekkesFørParkering = maksBeløpSomKanDekkesFørParkering,
    )
