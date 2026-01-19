package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

// TODO: Legg denne i egen fil - og finn nytt navn
data class UtgiftPrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reiseId: ReiseId,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val bompengerEnVei: Int?,
    val fergekostandEnVei: Int?,
) : Periode<LocalDate>,
    KopierPeriode<UtgiftPrivatBil> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): UtgiftPrivatBil = this.copy(fom = fom, tom = tom)
}

// Gjenstår:
// Ta hensyn til vedtaksperioder
// Ta det i bruk
// Skrive tester - dukker nok opp flere caser som ikke fungerer om man gjør det
// Tilpasse slik at beregningen kan brukes basert på kjørelister

@Service
class PrivatBilBeregningService {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
    ): BeregningsresultatPrivatBil {
        val reiseInformasjon = oppfylteVilkår.map { it.tilUtgiftPrivatbil() }

        return BeregningsresultatPrivatBil(
            reiser =
                reiseInformasjon.map {
                    beregnForReise(it, vedtaksperioder)
                },
        )
    }

    private fun beregnForReise(
        reise: UtgiftPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForReiseMedPrivatBil {
        // TODO: Er det nødvendig å justere vedtaksperiodene her? Annet enn at de brukes for å begrense reisen
        val (justerteVedtaksperioder, justertReiseperiode) =
            finnSnittMellomReiseOgVedtaksperioder(
                reise,
                vedtaksperioder,
            )

        val grunnlagForReise = lagBeregningsgrunnlagForReise(justertReiseperiode)

        val periodeSplittetPåUker = justertReiseperiode.splitPerUkeMedHelg()

        return BeregningsresultatForReiseMedPrivatBil(
            uker =
                periodeSplittetPåUker.mapNotNull {
                    beregnForUke(
                        uke = it,
                        grunnlagForReise = grunnlagForReise,
                        justerteVedtaksperioder,
                    )
                },
            grunnlag = grunnlagForReise,
        )
    }

    private fun beregnForUke(
        uke: PeriodeMedAntallDager,
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForUke? {
        val justertUke = justerUkeTilVedtaksperioder(uke, vedtaksperioder)

        val grunnlagForUke =
            lagBeregningsgrunnlagForUke(
                uke = justertUke,
                reisedagerPerUke = grunnlagForReise.reisedagerPerUke,
            )

        // Beregner ikke noe for uka dersom det ikke gjenstår noen dager i uka som er innafor en vedtaksperiode
        if (grunnlagForUke.antallDagerDenneUkaSomKanDekkes == 0) return null

        return BeregningsresultatForUke(
            grunnlag = grunnlagForUke,
            stønadsbeløp = beregnStønadsbeløp(grunnlagForReise = grunnlagForReise, grunnlagForUke = grunnlagForUke),
        )
    }

    private fun justerUkeTilVedtaksperioder(
        uke: PeriodeMedAntallDager,
        vedtaksperioder: List<Vedtaksperiode>,
    ): PeriodeMedAntallDager {
        val (relevanteVedtaksperioder, ukeMedJustertFomOgTom) =
            finnSnittMellomReiseOgVedtaksperioder(
                uke,
                vedtaksperioder,
            )
        val vedtaksperioderMedAntallDager = relevanteVedtaksperioder.map { it.finnAntallDager() }

        val ukeMedDagerBegrensetAvVedtaksperioder =
            PeriodeMedAntallDager(
                fom = ukeMedJustertFomOgTom.fom,
                tom = ukeMedJustertFomOgTom.tom,
                antallHverdager = vedtaksperioderMedAntallDager.sumOf { it.antallHverdager },
                antallHelgedager = vedtaksperioderMedAntallDager.sumOf { it.antallHelgedager },
            )

        validerJustertUke(ukeMedDagerBegrensetAvVedtaksperioder, uke)

        return ukeMedDagerBegrensetAvVedtaksperioder
    }

    private fun validerJustertUke(
        justertUke: PeriodeMedAntallDager,
        originalUke: PeriodeMedAntallDager,
    ) {
        feilHvis(justertUke.antallHverdager > originalUke.antallHverdager) {
            "Kan ikke ha flere hverdager i en uke etter justering enn originaluke"
        }

        feilHvis(justertUke.antallHelgedager > originalUke.antallHverdager) {
            "Kan ikke ha flere helgedager i en uke etter justering enn originaluke"
        }

        feilHvis(justertUke.antallHverdager > 5) {
            "Kan ikke ha mer enn 5 hverdager i en uke"
        }

        feilHvis(justertUke.antallHelgedager > 2) {
            "Kan ikke ha flere helgedager enn 2 i en uke"
        }
    }

    private fun Vedtaksperiode.finnAntallDager(): PeriodeMedAntallDager =
        PeriodeMedAntallDager(
            fom = this.fom,
            tom = this.tom,
            antallHverdager = antallHverdagerIPeriodeInklusiv(fom = fom, tom = tom),
            antallHelgedager = antallHelgedagerIPeriodeInklusiv(fom = fom, tom = tom),
        )

    private fun beregnStønadsbeløp(
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
        grunnlagForUke: BeregningsgrunnlagForUke,
    ): Int {
        val kostnadKjøring =
            grunnlagForReise.reiseavstandEnVei
                .multiply(BigDecimal.valueOf(2))
                .multiply(grunnlagForReise.kilometersats)
                .multiply(grunnlagForUke.antallDagerDenneUkaSomKanDekkes.toBigDecimal())

        val sumEkstrakostnader = grunnlagForReise.ekstrakostnader.beregnTotalEkstrakostnadForEnDag().toBigDecimal()

        val totaltBeløp = listOfNotNull(kostnadKjøring, sumEkstrakostnader).sumOf { it }

        return totaltBeløp.setScale(0, RoundingMode.HALF_UP).toInt()
    }

    private fun lagBeregningsgrunnlagForReise(reise: UtgiftPrivatBil): BeregningsgrunnlagForReiseMedPrivatBil =
        BeregningsgrunnlagForReiseMedPrivatBil(
            fom = reise.fom,
            tom = reise.tom,
            reisedagerPerUke = reise.reisedagerPerUke,
            reiseavstandEnVei = reise.reiseavstandEnVei,
            kilometersats = finnRelevantKilometerSats(periode = reise),
            ekstrakostnader =
                Ekstrakostnader(
                    fergekostnadEnVei = reise.fergekostandEnVei,
                    bompengerEnVei = reise.bompengerEnVei,
                ),
        )

    private fun lagBeregningsgrunnlagForUke(
        uke: PeriodeMedAntallDager,
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
            antallDagerInkludererHelg = uke.antallHelgedager > 0
        }

        return BeregningsgrunnlagForUke(
            fom = uke.fom,
            tom = uke.tom,
            antallDagerDenneUkaSomKanDekkes = antallDager,
            antallDagerInkludererHelg = antallDagerInkludererHelg,
        )
    }

    private fun VilkårDagligReise.tilUtgiftPrivatbil(): UtgiftPrivatBil {
        feilHvis(this.fakta !is FaktaPrivatBil) {
            "Forventer kun å få inn vilkår med fakta som er av type privat bil ved beregning av privat bil"
        }

        return UtgiftPrivatBil(
            fom = this.fom,
            tom = this.tom,
            reiseId = this.fakta.reiseId,
            reisedagerPerUke = this.fakta.reisedagerPerUke,
            reiseavstandEnVei = this.fakta.reiseavstandEnVei,
            bompengerEnVei = this.fakta.bompengerEnVei,
            fergekostandEnVei = this.fakta.fergekostandEnVei,
        )
    }
}
