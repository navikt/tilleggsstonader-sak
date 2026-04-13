package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilSatsForDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

data class RammevedtakPrivatBilDto(
    val reiser: List<RammeForReiseMedPrivatBilDto>,
)

data class RammeForReiseMedPrivatBilDto(
    val reiseId: ReiseId,
    val fom: LocalDate,
    val tom: LocalDate,
    val delperioder: List<DelperiodeDto>,
    val reiseavstandEnVei: BigDecimal,
    val aktivitetsadresse: String?,
)

data class DelperiodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
    val satser: List<RammeForReiseMedPrivatBilDelperiodeSatserDto>,
) : Periode<LocalDate>

data class RammeForReiseMedPrivatBilDelperiodeSatserDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal,
) : Periode<LocalDate>

fun RammevedtakPrivatBil.tilDto() =
    RammevedtakPrivatBilDto(
        reiser = reiser.map { it.tilDto() },
    )

fun RammeForReiseMedPrivatBil.tilDto(): RammeForReiseMedPrivatBilDto =
    RammeForReiseMedPrivatBilDto(
        reiseId = reiseId,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        delperioder =
            grunnlag.delperioder.map { delperiode ->
                DelperiodeDto(
                    fom = delperiode.fom,
                    tom = delperiode.tom,
                    bompengerPerDag = delperiode.ekstrakostnader.bompengerPerDag,
                    fergekostnadPerDag = delperiode.ekstrakostnader.fergekostnadPerDag,
                    reisedagerPerUke = delperiode.reisedagerPerUke,
                    satser = delperiode.satser.map { it.tilDto() },
                )
            },
        reiseavstandEnVei = grunnlag.reiseavstandEnVei,
        aktivitetsadresse = aktivitetsadresse,
    )

fun RammeForReiseMedPrivatBilSatsForDelperiode.tilDto() =
    RammeForReiseMedPrivatBilDelperiodeSatserDto(
        fom = fom,
        tom = tom,
        satsBekreftetVedVedtakstidspunkt = satsBekreftetVedVedtakstidspunkt,
        kilometersats = kilometersats,
        dagsatsUtenParkering = dagsatsUtenParkering,
    )
