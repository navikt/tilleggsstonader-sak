package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
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
    val uker: List<RammeForUke>,
    val grunnlag: BeregningsgrunnlagForReiseMedPrivatBil,
)

/**
 * dagsatsUtenParkering: hva brukeren kan få dekt per dag. Inkluderer bompenger og ferge, men ikke parkering.
 * maksBeløpSomKanDekkesFørParkering: maksimalt beløp bruker kan få dekt dersom hen kjører hver dag.
 */
data class RammeForUke(
    val grunnlag: BeregningsgrunnlagForUke,
    val dagsatsUtenParkering: BigDecimal,
    val maksBeløpSomKanDekkesFørParkering: BigInteger,
)

data class BeregningsgrunnlagForReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val ekstrakostnader: Ekstrakostnader,
) : Periode<LocalDate>

/**
 * fom: Mandag eller begrenset av reiseperioden
 * tom: Søndag eller begrenset av reiseperioden
 * maksAntallDagerSomKanDekkes: Begrenses av antall reisedager og fom/tom
 * antallDagerInkludererHelg: Flagg for å vise om beregning har tatt med helg i maksAntall dager
 * vedtaksperioder: Er en liste, men beregning håndterer foreløpig ikke mer enn 1
 * kilometersats: Hvor mye som dekkes per kilometer, fastsatt i forskriften og endres årlig.
 */
data class BeregningsgrunnlagForUke(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val maksAntallDagerSomKanDekkes: Int,
    val antallDagerInkludererHelg: Boolean,
    val vedtaksperioder: List<Vedtaksperiode>,
    val kilometersats: BigDecimal,
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
