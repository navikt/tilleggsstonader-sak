import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

data class BeregningsresultatOffentligTransport(
    val perioder: List<BeregningsresultatPerLøpendeMåned>,
)

data class BeregningsresultatPerLøpendeMåned(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beløp: Int,
    val grunnlag: List<BergningsGrunnlag>,
//    val bilettKombinasjonen: BilettKombinasjon
) : Periode<LocalDate>

data class BergningsGrunnlag(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallReisedagerPerUke: Int,
    val prisEnkelbillett: Int,
    val pris30dagersbillett: Int,
    val pris7dagersbillett: Int,
    val beregnetTidsunkt: LocalDate = LocalDate.now(),
)
