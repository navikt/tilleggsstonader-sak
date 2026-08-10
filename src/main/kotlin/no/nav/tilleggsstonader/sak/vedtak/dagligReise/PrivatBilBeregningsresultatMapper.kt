package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.RammeForReiseMedPrivatBilDelperiodeSatserDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.sats.satser
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
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

fun BeregningsresultatForReisePrivatBil.oppsummerReise(rammevedtakForReise: RammevedtakForReiseMedPrivatBil) =
    OppsummertBeregningForReiseDto(
        reiseId = this.reiseId,
        reiseavstandEnVei = rammevedtakForReise.grunnlag.reiseavstandEnVei,
        aktivitetsadresse = rammevedtakForReise.aktivitetsadresse,
        perioder = this.perioder.map { it.oppsummerPeriode(rammevedtakForReise) }.sortedBy { it.ukenummer },
        fraTidligereVedtak = this.perioder.all { it.fraTidligereVedtak },
    )

private fun BeregningsresultatForReisePrivatBilPeriode.oppsummerPeriode(
    rammevedtakForReise: RammevedtakForReiseMedPrivatBil,
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
        bompengerTotalt = relevantDelperiode.ekstrakostnader.bompengerPerDag?.times(antallGodkjenteReisedager.toBigDecimal()),
        fergekostnadTotalt = relevantDelperiode.ekstrakostnader.fergekostnadPerDag?.times(antallGodkjenteReisedager.toBigDecimal()),
        satser = relevanteSatser,
        parkeringskostnadTotalt = this.grunnlag.dager.sumOf { it.parkeringskostnad },
        stønadsbeløp = this.stønadsbeløp,
        fraTidligereVedtak = this.fraTidligereVedtak,
    )
}

fun PrivatBilOppsummertBeregningDto.finnSatserBruktIBeregning(): List<SatsPrivatBil> =
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
    val fraTidligereVedtak: Boolean,
) {
    val totaltStønadsbeløpMedPerioderFraForrigeVedtak = perioder.sumOf { it.stønadsbeløp }
    val totaltStønadsbeløpUtenPerioderFraForrigeVedtak =
        perioder.filter { !it.fraTidligereVedtak }.sumOf { it.stønadsbeløp }
}

data class OppsummertBeregningForPeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallGodkjenteReisedager: Int,
    val bompengerTotalt: BigDecimal?,
    val fergekostnadTotalt: BigDecimal?,
    val satser: List<RammeForReiseMedPrivatBilDelperiodeSatserDto>,
    val parkeringskostnadTotalt: Int,
    val stønadsbeløp: BigDecimal,
    val fraTidligereVedtak: Boolean,
) {
    val ukenummer = fom.ukenummer()
}

private fun List<OppsummertBeregningForPeriodeDto>.mergeSammenhengende(): List<Datoperiode> =
    this
        .map { Datoperiode(it.fom, it.tom) }
        .sorted()
        .mergeSammenhengende { v1, v2 -> v1.påfølgesAv(v2) }
