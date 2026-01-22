package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.math.BigDecimal
import java.time.LocalDate

// Generelle tanker:
// Mangler informasjon om vedtaksperiode
// Burde vi splitte opp reisene slik at det blir 1 ny ved nytt år?
data class BeregningsresultatPrivatBil(
    val reiser: List<BeregningsresultatForReiseMedPrivatBil>,
)

// Vil oppnå:
// Enkelt å sjekke om kun ramme er beregnet
// Unngå duplikat lagring av data
// Smud å dytte inn kjøreliste og ekte resultat når rammevedtak alt er beregnet

// TODO: Vurder om det er nødvendig å ha med fom og tom her
data class BeregningsresultatForReiseMedPrivatBil(
    val uker: List<BeregningsresultatForUke>,
    val grunnlag: BeregningsgrunnlagForReiseMedPrivatBil,
)

// Vedtaksperiode inn her?
data class BeregningsresultatForUke(
    val grunnlag: BeregningsgrunnlagForUke,
    // TODO: Vurder navn - Gir dette navnet mening når det er etterbetaling?
    val stønadsbeløp: Int,
//    val maksBeløpSomKanDekkes: Int?,
)

// TODO: Finn ut om tall generelt skal være heltall eller desimaltall
// Antar heltall på alt utenom kilometersats foreløpig
data class BeregningsgrunnlagForReiseMedPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    // TODO: Vurder om denne burde ligge på uke. Avhenger av hvor vi vil splitte en reise
    val kilometersats: BigDecimal,
    val ekstrakostnader: Ekstrakostnader,
) : Periode<LocalDate>

data class BeregningsgrunnlagForUke(
    override val fom: LocalDate, // mandag eller begrenset av reiseperioden
    override val tom: LocalDate, // søndag eller begrenset av reiseperioden
    val kjøreliste: GrunnlagKjøreliste? = null,
    // TODO: Bytt til et bedre navn: antallReisedager? maksAntallReisedager?
    // reisedager per uke, begrenset av antall dager som er i uka (første og siste uke er ikke nødvendigvis fulle uker)
    val antallDagerDenneUkaSomKanDekkes: Int,
    // Ikke så bra navn, men betyr at utregningen av den over inkluderer en eller to helgedager
    val antallDagerInkludererHelg: Boolean,
    // TODO: Vi håndterer ikke at denne er mer enn 1, men kjipt å ikke ha den som en liste dersom vi vil det senere
    val vedtaksperioder: List<Vedtaksperiode>,
    // val dagsats: BigDecimal, //TODO: Lag
) : Periode<LocalDate>

data class Ekstrakostnader(
    val bompengerEnVei: Int?, // TODO: Kan man ha noe abonnement/månedskort her? Tar vi hensyn til det?
    val fergekostnadEnVei: Int?, // Månedskort?
) {
    // TODO: Spør om hjelp til å skrive denne penere
    fun beregnTotalEkstrakostnadForEnDag(): Int {
        val bompengerEnDag = bompengerEnVei?.times(2) ?: 0
        val fergekostnadEnDag = fergekostnadEnVei?.times(2) ?: 0

        return bompengerEnDag + fergekostnadEnDag
    }
}

data class GrunnlagKjøreliste(
    val antallDagerKjørt: Int,
    val totaleParkeringsutgifter: Int?,
)
