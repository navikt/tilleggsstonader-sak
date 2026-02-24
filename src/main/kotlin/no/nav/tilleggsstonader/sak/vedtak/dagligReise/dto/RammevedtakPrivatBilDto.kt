package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.libs.utils.dato.alleDatoerGruppertPåUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
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
)

fun RammevedtakPrivatBil.tilDto() =
    RammevedtakPrivatBilDto(
        reiser = reiser.flatMap { it.tilDto() },
    )

// TODO: Flytt splitting til beregning dersom vi vil beholde det
private fun RammeForReiseMedPrivatBil.tilDto(): List<RammeForReiseMedPrivatBilDto> {
    return grunnlag.satser.map {
        RammeForReiseMedPrivatBilDto(
            reiseId = reiseId,
            fom = it.fom,
            tom = it.tom,
            reisedagerPerUke = grunnlag.reisedagerPerUke,
            reiseavstandEnVei = grunnlag.reiseavstandEnVei,
            bompengerEnVei = grunnlag.ekstrakostnader.bompengerEnVei,
            kilometersats = it.kilometersats,
            dagsatsUtenParkering = it.dagsatsUtenParkering,
            fergekostnadEnVei = grunnlag.ekstrakostnader.fergekostnadEnVei,
            aktivitetsadresse = this.aktivitetsadresse,
        )
    }
}
