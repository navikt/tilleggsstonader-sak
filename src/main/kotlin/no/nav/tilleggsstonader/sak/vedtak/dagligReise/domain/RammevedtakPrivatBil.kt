package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class RammevedtakPrivatBil(
    val reiser: List<RammeForReiseMedPrivatBil>,
)

data class RammeForReiseMedPrivatBil(
    val reiseId: ReiseId,
    val aktivitetsadresse: String?,
    val grunnlag: BeregningsgrunnlagForReiseMedPrivatBil,
)


data class BeregningsgrunnlagForReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val ekstrakostnader: Ekstrakostnader,
    val satser: List<SatsForPeriodePrivatBil>
) : Periode<LocalDate>

// TODO: Finn ut om det finnes abbonnement på disse prisene og om det påvirker hvordan vi vil løse dette
data class Ekstrakostnader(
    val bompengerEnVei: Int?,
    val fergekostnadEnVei: Int?,
) {
    fun beregnTotalEkstrakostnadForEnDag(): BigDecimal {
        val bompengerEnDag = bompengerEnVei?.times(2) ?: 0
        val fergekostnadEnDag = fergekostnadEnVei?.times(2) ?: 0

        val sum = bompengerEnDag + fergekostnadEnDag

        return sum.toBigDecimal()
    }
}

/**
 * dagsatsUtenParkering: hva brukeren kan få dekt per dag. Inkluderer bompenger og ferge, men ikke parkering.
 * maksBeløpSomKanDekkesFørParkering: maksimalt beløp bruker kan få dekt dersom hen kjører hver dag.
 */
data class SatsForPeriodePrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal,
): Periode<LocalDate>