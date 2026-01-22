package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.PeriodeMedAntallDager
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.antallHelgedagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.antallHverdagerIPeriodeInklusiv
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.finnSnittMellomReiseOgVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.splitPerUkeMedHelg
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

@Service
class PrivatBilBeregningService {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
    ): BeregningsresultatPrivatBil {
        val reiseInformasjon = oppfylteVilkår.map { it.tilUtgiftPrivatbil() }

        return BeregningsresultatPrivatBil(
            reiser =
                reiseInformasjon.mapNotNull {
                    beregnForReise(it, vedtaksperioder)
                },
        )
    }

    private fun beregnForReise(
        reise: UtgiftPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForReiseMedPrivatBil? {
        val (_, justertReise) = finnSnittMellomReiseOgVedtaksperioder(reise, vedtaksperioder)

        if (justertReise == null) return null

        val grunnlagForReise = lagBeregningsgrunnlagForReise(justertReise)

        val periodeSplittetPåUker = justertReise.splitPerUkeMedHelg()

        return BeregningsresultatForReiseMedPrivatBil(
            uker =
                periodeSplittetPåUker.mapNotNull {
                    beregnForUke(
                        uke = it,
                        grunnlagForReise = grunnlagForReise,
                        vedtaksperioder = vedtaksperioder,
                    )
                },
            grunnlag = grunnlagForReise,
        )
    }

    private fun beregnForUke(
        uke: Datoperiode,
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatForUke? {
        val grunnlagForUke =
            lagBeregningsgrunnlagForUke(
                uke = uke,
                reisedagerPerUke = grunnlagForReise.reisedagerPerUke,
                vedtaksperioder = vedtaksperioder,
            ) ?: return null

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
        uke: Datoperiode,
        reisedagerPerUke: Int,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsgrunnlagForUke? {
        val relevantVedtaksperiode = finnRelevantVedtaksperiodeForUke(uke, vedtaksperioder) ?: return null

        val justertUkeMedAntallDager = uke.tilpassUkeTilVedtaksperiode(relevantVedtaksperiode) ?: return null

        val (antallDager, antallDagerInkludererHelg) = finnAntallDagerSomDekkes(justertUkeMedAntallDager, reisedagerPerUke)

        return BeregningsgrunnlagForUke(
            fom = justertUkeMedAntallDager.fom,
            tom = justertUkeMedAntallDager.tom,
            antallDagerDenneUkaSomKanDekkes = antallDager,
            antallDagerInkludererHelg = antallDagerInkludererHelg,
            vedtaksperioder = listOf(relevantVedtaksperiode),
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
