import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.ReiseInformasjon
import java.time.LocalDate

data class BeregningsresultatOffentligTransport(
    val perioder: List<BeregningsresultatPer30Dagersperiode>,
)

data class BeregningsresultatPer30Dagersperiode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val beregningsresultatTransportmiddel: List<BeregningsresultatTransportmiddel>,
    val summertBeløp: Int,
    val grunnlag: List<BergningsGrunnlag>,
) : Periode<LocalDate>

data class BeregningsresultatTransportmiddel(
    val kilde: String,
    val beløp: Int,
    // val bilettKombinasjonen: BilettKombinasjon,
)

// data class BilettKombinasjon(
//    val antallEnkeltbilletter: Int,
//    val antall7dagersbilletter: Int,
//    val antall30dagersbilletter: Int,
// )

data class BergningsGrunnlag(
    val fom: LocalDate,
    val tom: LocalDate,
    val reiseInformasjon: List<ReiseInformasjon>,
)
