package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

data class RammevedtakPrivatBil(
    val reiser: List<RammeForReiseMedPrivatBil>,
) {
    fun hentRammevedtakForReise(reiseId: ReiseId): RammeForReiseMedPrivatBil = reiser.single { it.reiseId == reiseId }
}

data class RammeForReiseMedPrivatBil(
    val reiseId: ReiseId,
    val aktivitetsadresse: String?,
    val grunnlag: BeregningsgrunnlagForReiseMedPrivatBil,
)

data class BeregningsgrunnlagForReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val delPerioder: List<Delperiode>,
    val reiseavstandEnVei: BigDecimal,
    val vedtaksperioder: List<Vedtaksperiode>,
) : Periode<LocalDate> {
    fun vedtaksperiodeForPeriode(periode: Periode<LocalDate>) = vedtaksperioder.single { it.inneholder(periode) }
}

data class Delperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val ekstrakostnader: Ekstrakostnader,
    val reisedagerPerUke: Int,
    val satsBekreftetVedVedtakstidspunkt: Boolean,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal, // hva brukeren kan få dekt per dag. Inkluderer bompenger og ferge, men ikke parkering.
) : Periode<LocalDate>

// TODO: Finn ut om det finnes abbonnement på disse prisene og om det påvirker hvordan vi vil løse dette
data class Ekstrakostnader(
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
) {
    fun beregnTotalEkstrakostnadForEnDag(): BigDecimal {
        val bompengerEnDag = bompengerPerDag ?: 0
        val fergekostnadEnDag = fergekostnadPerDag ?: 0

        val sum = bompengerEnDag + fergekostnadEnDag

        return sum.toBigDecimal()
    }
}
