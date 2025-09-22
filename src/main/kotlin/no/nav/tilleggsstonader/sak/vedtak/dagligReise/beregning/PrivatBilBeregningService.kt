package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class DummyReiseMedBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: Int,
    val dagligParkeringsutgift: Int?,
    val bompengerEnVei: Int?,
    val dagligPiggdekkavgift: Int?,
    val fergekostnadEnVei: Int?,
) : Periode<LocalDate>

// Gjenstår:
// Ta hensyn til vedtaksperioder
// Ta det i bruk
// Skrive tester - dukker nok opp flere caser som ikke fungerer om man gjør det
// Tilpasse slik at beregningen kan brukes basert på kjørelister

class PrivatBilBeregningService {
    fun beregn(reiser: List<DummyReiseMedBil>): BeregningsresultatPrivatBil =
        BeregningsresultatPrivatBil(
            reiser = reiser.map { beregnForReise(it) },
        )

    private fun beregnForReise(reise: DummyReiseMedBil): BeregningsresultatForReiseMedPrivatBil {
        val grunnlagForReise = lagBeregningsgrunnlagForReise(reise)

        val periodeSplittetPåUker = reise.splitPerUkeMedHelg()

        return BeregningsresultatForReiseMedPrivatBil(
            uker = periodeSplittetPåUker.map { beregnForUke(uke = it, grunnlagForReise = grunnlagForReise) },
            grunnlag = grunnlagForReise,
        )
    }

    private fun beregnForUke(
        uke: UkeMedAntallDager,
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
    ): BeregningsresultatForUke {
        val grunnlagForUke =
            lagBeregningsgrunnlagForUke(uke = uke, reisedagerPerUke = grunnlagForReise.reisedagerPerUke)

        return BeregningsresultatForUke(
            grunnlag = grunnlagForUke,
            stønadsbeløp = beregnStønadsbeløp(grunnlagForReise = grunnlagForReise, grunnlagForUke = grunnlagForUke),
        )
    }

    private fun beregnStønadsbeløp(
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
        grunnlagForUke: BeregningsgrunnlagForUke,
    ): Int {
        val kostnadKjøring =
            grunnlagForReise.reiseavstandEnVei
                .toBigDecimal()
                .multiply(BigDecimal.valueOf(2))
                .multiply(grunnlagForReise.kilometersats)
                .multiply(grunnlagForUke.antallDagerDenneUkaSomKanDekkes.toBigDecimal())

        val parkeringskostnad =
            grunnlagForReise.dagligParkeringsutgift
                ?.times(grunnlagForUke.antallDagerDenneUkaSomKanDekkes)
                ?.toBigDecimal()

        val sumEkstrakostnader = grunnlagForReise.ekstrakostnader.beregnTotalEkstrakostnadForEnDag().toBigDecimal()

        val totaltBeløp = listOfNotNull(kostnadKjøring, parkeringskostnad, sumEkstrakostnader).sumOf { it }

        return totaltBeløp.setScale(0, RoundingMode.HALF_UP).toInt()
    }

    private fun lagBeregningsgrunnlagForReise(reise: DummyReiseMedBil): BeregningsgrunnlagForReiseMedPrivatBil =
        BeregningsgrunnlagForReiseMedPrivatBil(
            fom = reise.fom,
            tom = reise.tom,
            reisedagerPerUke = reise.reisedagerPerUke,
            reiseavstandEnVei = reise.reiseavstandEnVei,
            dagligParkeringsutgift = reise.dagligParkeringsutgift,
            kilometersats = finnRelevantKilometerSats(periode = reise),
            ekstrakostnader =
                Ekstrakostnader(
                    fergekostnadEnVei = reise.fergekostnadEnVei,
                    bompengerEnVei = reise.bompengerEnVei,
                    dagligPiggdekkavgift = reise.dagligPiggdekkavgift,
                ),
        )

    private fun lagBeregningsgrunnlagForUke(
        uke: UkeMedAntallDager,
        reisedagerPerUke: Int,
    ): BeregningsgrunnlagForUke {
        var antallDager: Int
        var antallDagerInkludererHelg: Boolean

        val totaltAntallDagerIUke = uke.antallHverdager + uke.antallHelgedager

        if (reisedagerPerUke <= uke.antallHverdager) {
            antallDager = reisedagerPerUke
            antallDagerInkludererHelg = false
        } else if (reisedagerPerUke <= totaltAntallDagerIUke) {
            antallDager = reisedagerPerUke
            antallDagerInkludererHelg = true
        } else {
            antallDager = totaltAntallDagerIUke
            antallDagerInkludererHelg = true
        }

        return BeregningsgrunnlagForUke(
            fom = uke.fom,
            tom = uke.tom,
            antallDagerDenneUkaSomKanDekkes = antallDager,
            antallDagerInkludererHelg = antallDagerInkludererHelg,
        )
    }
}
