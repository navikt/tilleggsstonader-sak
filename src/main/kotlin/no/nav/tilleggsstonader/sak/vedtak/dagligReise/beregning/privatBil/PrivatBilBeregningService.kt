package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.finnSnittMellomReiseOgVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

// Begrensninger:
// Håndterer ikke ulik kilometersats i årskifte dersom en uke går på tvers av to år.

@Service
class PrivatBilBeregningService {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
    ): BeregningsresultatPrivatBil {
        val reiseInformasjon = oppfylteVilkår.map { it.tilReiseMedPrivatBil() }

        val resultatForReiser = reiseInformasjon.mapNotNull {
            beregnForReise(it, vedtaksperioder)
        }

        return BeregningsresultatPrivatBil(
            reiser = resultatForReiser,
        )
    }

    private fun beregnForReise(
        reise: ReiseMedPrivatBil,
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
            maksBeløpSomKanDekkesFørParkering = beregnStønadsbeløp(grunnlagForReise = grunnlagForReise, grunnlagForUke = grunnlagForUke),
        )
    }

    private fun beregnStønadsbeløp(
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
        grunnlagForUke: BeregningsgrunnlagForUke,
    ): Int {
        val kostnadKjøring =
            grunnlagForReise.reiseavstandEnVei
                .multiply(BigDecimal.valueOf(2))
                .multiply(grunnlagForUke.kilometersats)
                .multiply(grunnlagForUke.maksAntallDagerSomKanDekkes.toBigDecimal())

        val sumEkstrakostnader = grunnlagForReise.ekstrakostnader.beregnTotalEkstrakostnadForEnDag().toBigDecimal()

        val totaltBeløp = listOfNotNull(kostnadKjøring, sumEkstrakostnader).sumOf { it }

        return totaltBeløp.setScale(0, RoundingMode.HALF_UP).toInt()
    }

    private fun lagBeregningsgrunnlagForReise(reise: ReiseMedPrivatBil): BeregningsgrunnlagForReiseMedPrivatBil =
        BeregningsgrunnlagForReiseMedPrivatBil(
            fom = reise.fom,
            tom = reise.tom,
            reisedagerPerUke = reise.reisedagerPerUke,
            reiseavstandEnVei = reise.reiseavstandEnVei,
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
            maksAntallDagerSomKanDekkes = antallDager,
            antallDagerInkludererHelg = antallDagerInkludererHelg,
            vedtaksperioder = listOf(relevantVedtaksperiode),
            kilometersats = finnRelevantKilometerSats(periode = justertUkeMedAntallDager),
        )
    }
}
