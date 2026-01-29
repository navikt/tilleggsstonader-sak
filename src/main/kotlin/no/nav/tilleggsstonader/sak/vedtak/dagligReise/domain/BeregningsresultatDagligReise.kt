package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatDagligReise(
    val offentligTransport: BeregningsresultatOffentligTransport?,
    val privatBil: BeregningsresultatPrivatBil? = null,
)

// Generelle tanker:
// Burde vi splitte opp reisene slik at det blir 1 ny ved nytt år?
data class BeregningsresultatPrivatBil(
    val reiser: List<BeregningsresultatForReiseMedPrivatBil>,
)

data class BeregningsresultatForReiseMedPrivatBil(
    val uker: List<BeregningsresultatForUke>,
    val grunnlag: BeregningsgrunnlagForReiseMedPrivatBil,
)

data class BeregningsresultatForUke(
    val grunnlag: BeregningsgrunnlagForUke,
    val maksBeløpSomKanDekkesFørParkering: Int,
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
 */
data class BeregningsgrunnlagForUke(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val maksAntallDagerSomKanDekkes: Int,
    val antallDagerInkludererHelg: Boolean,
    val vedtaksperioder: List<Vedtaksperiode>,
    val kilometersats: BigDecimal,
    val dagsatsUtenParkering: BigDecimal,
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
