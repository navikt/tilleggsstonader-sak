package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
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
    val ekstrakostnader: EkstrakostnaderDto,
    val reisedagerPerUke: Int,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal, // hva brukeren kan få dekt per dag. Inkluderer bompenger og ferge, men ikke parkering.
) : Periode<LocalDate>

data class EkstrakostnaderDto(
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
)

fun RammevedtakPrivatBil.tilDto() =
    RammevedtakPrivatBilDto(
        reiser = reiser.flatMap { it.tilDto() },
    )

// TODO: Flytt splitting til beregning dersom vi vil beholde det
fun RammeForReiseMedPrivatBil.tilDto(): List<RammeForReiseMedPrivatBilDto> =
    grunnlag.delPerioder.map {
        RammeForReiseMedPrivatBilDto(
            reiseId = reiseId,
            fom = it.fom,
            tom = it.tom,
            delperioder =
                this.grunnlag.delPerioder.map { delperiode ->
                    DelperiodeDto(
                        fom = delperiode.fom,
                        tom = delperiode.tom,
                        ekstrakostnader =
                            EkstrakostnaderDto(
                                bompengerPerDag = delperiode.ekstrakostnader.bompengerPerDag,
                                fergekostnadPerDag = delperiode.ekstrakostnader.fergekostnadPerDag,
                            ),
                        reisedagerPerUke = delperiode.reisedagerPerUke,
                        satsBekreftetVedVedtakstidspunkt = delperiode.satsBekreftetVedVedtakstidspunkt,
                        kilometersats = delperiode.kilometersats,
                        dagsatsUtenParkering = delperiode.dagsatsUtenParkering,
                    )
                },
            reiseavstandEnVei = grunnlag.reiseavstandEnVei,
            aktivitetsadresse = this.aktivitetsadresse,
        )
    }
