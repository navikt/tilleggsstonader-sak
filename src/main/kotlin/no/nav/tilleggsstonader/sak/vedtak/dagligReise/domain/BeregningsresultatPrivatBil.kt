package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.math.BigDecimal
import java.time.LocalDate

// Generelle tanker:
// Mangler informasjon om vedtaksperiode
// Burde vi splitte opp reisene slik at det blir 1 ny ved nytt år?
data class BeregningsresultatPrivatBil(
    val reiser: List<BeregningsresultatForReiseMedPrivatBil>,
)

// TODO: Vurder om det er nødvendig å ha med fom og tom her
data class BeregningsresultatForReiseMedPrivatBil(
    val uker: List<BeregningsresultatForUke>,
    val grunnlag: BeregningsgrunnlagForReiseMedPrivatBil,
)

// Vedtaksperiode inn her?
data class BeregningsresultatForUke(
    val grunnlag: BeregningsgrunnlagForUke,
    val stønadsbeløp: BigDecimal, // TODO: Vurder navn - Gir dette navnet mening når det er etterbetaling?
)

// TODO: Finn ut om tall generelt skal være heltall eller desimaltall
// Antar heltall på alt utenom kilometersats foreløpig
data class BeregningsgrunnlagForReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: Int,
    val kilometersats: BigDecimal, // TODO: Vurder om denne burde ligge på uke. Avhenger av hvor vi vil splitte en reise
    val dagligParkeringsutgift: Int?, // TODO: Vurder om denne burde ligge inni Ekstrakostnader
    val ekstrakostnader: Ekstrakostnader,
) : Periode<LocalDate>

data class BeregningsgrunnlagForUke(
    override val fom: LocalDate, // mandag eller begrenset av reiseperioden
    override val tom: LocalDate, // søndag eller fredag?
    val kjøreliste: GrunnlagKjøreliste? = null,
    // TODO: Bytt til et bedre navn: antallReisedager? maksAntallReisedager?
    // reisedager per uke, begrenset av antall dager som er i uka (første og siste uke er ikke nødvendigvis fulle uker)
    val antallDagerDenneUkaSomKanDekkes: Int,
    // Ikke så bra navn, men betyr at utregningen av den over inkluderer en eller to helgedager
    val antallDagerInkludererHelg: Boolean,
) : Periode<LocalDate>

data class Ekstrakostnader(
    val bompengerEnVei: Int?, // TODO: Kan man ha noe abonnement/månedskort her? Tar vi hensyn til det?
    val dagligPiggdekkavgift: Int?, // TODO: Legges denne inn som en fast sum per dag eller totalsum for periode?
    val fergekostnadEnVei: Int?, // Månedskort?
) {
    // TODO: Spør om hjelp til å skrive denne penere
    fun beregnTotalEkstrakostnadForEnDag(): Int {
        val bompengerEnDag = bompengerEnVei?.times(2) ?: 0
        val fergekostnadEnDag = fergekostnadEnVei?.times(2) ?: 0
        val piggdekk = dagligPiggdekkavgift ?: 0

        return bompengerEnDag + fergekostnadEnDag + piggdekk
    }

// Er dette feks bedre?
//    fun beregnTotalEkstrakostnadForEnDag3(): Int {
//        return listOfNotNull(
//            bompengerEnVei?.takeIf { it > 0 }?.times(2),
//            fergekostnadEnVei?.takeIf { it > 0 }?.times(2),
//            dagligPiggdekkavgift?.takeIf { it > 0 }
//        ).sum()
//    }
}

data class GrunnlagKjøreliste(
    val antallDagerKjørt: Int,
    val totaleParkeringsutgifter: Int?,
)
