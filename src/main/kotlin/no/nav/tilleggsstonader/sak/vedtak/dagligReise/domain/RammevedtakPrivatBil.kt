package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
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
    val aktivitetType: AktivitetType,
    val typeAktivitet: TypeAktivitet?,
    val grunnlag: BeregningsgrunnlagForReiseMedPrivatBil,
)

data class BeregningsgrunnlagForReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val ekstrakostnader: Ekstrakostnader,
    val satser: List<SatsForPeriodePrivatBil>,
    val vedtaksperioder: List<Vedtaksperiode>,
) : Periode<LocalDate> {
    fun vedtaksperiodeForPeriode(periode: Periode<LocalDate>) = vedtaksperioder.single { it.inneholder(periode) }
}

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
) : Periode<LocalDate>
