package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Mergeable
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.SatsDagligReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.satser
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.RammeForReiseMedPrivatBilDelperiodeSatserDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

fun oppsummerBeregningPrivatBil(
    beregningsresultatPrivatBil: BeregningsresultatPrivatBil,
    rammevedtak: RammevedtakPrivatBil,
): PrivatBilOppsummertBeregningDto =
    PrivatBilOppsummertBeregningDto(
        reiser =
            beregningsresultatPrivatBil.reiser.map { beregningsresultatForReise ->
                val rammevedtakForReise = rammevedtak.reiser.single { it.reiseId == beregningsresultatForReise.reiseId }

                beregningsresultatForReise.oppsummerReise(rammevedtakForReise)
            },
    )

private fun BeregningsresultatForReisePrivatBil.oppsummerReise(rammevedtakForReise: RammeForReiseMedPrivatBil) =
    OppsummertBeregningForReiseDto(
        reiseId = this.reiseId,
        reiseavstandEnVei = rammevedtakForReise.grunnlag.reiseavstandEnVei,
        aktivitetsadresse = rammevedtakForReise.aktivitetsadresse,
        perioder = this.perioder.map { it.oppsummerPeriode(rammevedtakForReise) },
    )

private fun BeregningsresultatForReisePrivatBilPeriode.oppsummerPeriode(
    rammevedtakForReise: RammeForReiseMedPrivatBil,
): OppsummertBeregningForPeriodeDto {
    val relevantDelperiode = rammevedtakForReise.finnDelperiodeForPeriode(this)
    val relevanteSatser =
        relevantDelperiode.satser
            .filter { it.overlapper(this) }
            .map { it.tilDto() }

    val antallGodkjenteReisedager = this.grunnlag.dager.count()

    return OppsummertBeregningForPeriodeDto(
        fom = this.fom,
        tom = this.tom,
        antallGodkjenteReisedager = antallGodkjenteReisedager,
        bompengerTotalt = relevantDelperiode.ekstrakostnader.bompengerPerDag?.times(antallGodkjenteReisedager),
        fergekostnadTotalt = relevantDelperiode.ekstrakostnader.fergekostnadPerDag?.times(antallGodkjenteReisedager),
        satser = relevanteSatser,
        parkeringskostnadTotalt = this.grunnlag.dager.sumOf { it.parkeringskostnad },
        stønadsbeløp = this.stønadsbeløp,
    )
}

fun PrivatBilOppsummertBeregningDto.finnSatserBruktIBeregning(): List<SatsDagligReisePrivatBil> =
    reiser
        .flatMap { reise ->
            reise.perioder.mergeSammenhengende().flatMap { periode ->
                satser.filter { it.overlapper(periode) }
            }
        }.distinct()
        .sorted()

data class PrivatBilOppsummertBeregningDto(
    val reiser: List<OppsummertBeregningForReiseDto>,
)

data class OppsummertBeregningForReiseDto(
    val reiseId: ReiseId,
    val reiseavstandEnVei: BigDecimal,
    val aktivitetsadresse: String?,
    val perioder: List<OppsummertBeregningForPeriodeDto>,
) {
    val totaltStønadsbeløp = perioder.sumOf { it.stønadsbeløp }
}

data class OppsummertBeregningForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val antallGodkjenteReisedager: Int,
    val bompengerTotalt: Int?,
    val fergekostnadTotalt: Int?,
    val satser: List<RammeForReiseMedPrivatBilDelperiodeSatserDto>,
    val parkeringskostnadTotalt: Int,
    val stønadsbeløp: BigDecimal,
) : Periode<LocalDate>,
    Mergeable<LocalDate, OppsummertBeregningForPeriodeDto> {
    val ukenummer = fom.ukenummer()

    override fun merge(other: OppsummertBeregningForPeriodeDto): OppsummertBeregningForPeriodeDto =
        this.copy(fom = minOf(this.fom, other.fom), tom = maxOf(this.tom, other.tom))
}

fun List<OppsummertBeregningForPeriodeDto>.mergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende { v1, v2 -> v1.påfølgesAv(v2) }
