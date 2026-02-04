package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.finnSnittMellomReiseOgVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

// Begrensninger:
// Håndterer ikke ulik kilometersats i årskifte dersom en uke går på tvers av to år.

@Service
class PrivatBilBeregningService {
    fun beregnRammevedtak(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
    ): RammevedtakPrivatBil? {
        val reiseInformasjon = oppfylteVilkår.map { it.tilReiseMedPrivatBil() }

        val resultatForReiser =
            reiseInformasjon.mapNotNull {
                beregnForReise(it, vedtaksperioder)
            }

        if (resultatForReiser.isEmpty()) return null

        return RammevedtakPrivatBil(
            reiser = resultatForReiser,
        )
    }

    private fun beregnForReise(
        reise: ReiseMedPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): RammeForReiseMedPrivatBil? {
        val justertReise =
            finnSnittMellomReiseOgVedtaksperioder(reise, vedtaksperioder).justertReiseperiode ?: return null

        val grunnlagForReise = lagBeregningsgrunnlagForReise(justertReise)

        val periodeSplittetPåUker = justertReise.splitPerUkeMedHelg()

        val uker =
            periodeSplittetPåUker.mapNotNull {
                beregnForUke(
                    uke = it,
                    grunnlagForReise = grunnlagForReise,
                    vedtaksperioder = vedtaksperioder,
                )
            }

        if (uker.isEmpty()) return null

        return RammeForReiseMedPrivatBil(
            reiseId = reise.reiseId,
            aktivitetsadresse = reise.aktivitetsadresse,
            uker = uker,
            grunnlag = grunnlagForReise,
        )
    }

    private fun beregnForUke(
        uke: Datoperiode,
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): RammeForUke? {
        val grunnlagForUke =
            lagBeregningsgrunnlagForUke(
                uke = uke,
                reisedagerPerUke = grunnlagForReise.reisedagerPerUke,
                vedtaksperioder = vedtaksperioder,
            ) ?: return null

        val dagsatsUtenParkering =
            beregnDagsatsUtenParkering(
                kilometersats = grunnlagForUke.kilometersats,
                grunnlagForReise = grunnlagForReise,
            )

        return RammeForUke(
            grunnlag = grunnlagForUke,
            dagsatsUtenParkering = dagsatsUtenParkering,
            maksBeløpSomKanDekkesFørParkering =
                beregnMaksbeløp(
                    grunnlagForUke = grunnlagForUke,
                    dagsatsUtenParkering = dagsatsUtenParkering,
                ),
        )
    }

    private fun beregnMaksbeløp(
        grunnlagForUke: BeregningsgrunnlagForUke,
        dagsatsUtenParkering: BigDecimal,
    ) = dagsatsUtenParkering
        .multiply(grunnlagForUke.maksAntallDagerSomKanDekkes.toBigDecimal())
        .setScale(0, RoundingMode.HALF_UP)
        .toBigInteger()

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

        val justertUkeMedAntallDager =
            uke.finnAntallDagerIUkeInnenforVedtaksperiode(relevantVedtaksperiode) ?: return null

        val (antallDager, antallDagerInkludererHelg) =
            finnAntallDagerSomDekkes(
                justertUkeMedAntallDager,
                reisedagerPerUke,
            )

        return BeregningsgrunnlagForUke(
            fom = justertUkeMedAntallDager.fom,
            tom = justertUkeMedAntallDager.tom,
            maksAntallDagerSomKanDekkes = antallDager,
            antallDagerInkludererHelg = antallDagerInkludererHelg,
            vedtaksperioder = listOf(relevantVedtaksperiode),
            kilometersats = finnRelevantKilometerSats(periode = justertUkeMedAntallDager),
        )
    }

    private fun beregnDagsatsUtenParkering(
        grunnlagForReise: BeregningsgrunnlagForReiseMedPrivatBil,
        kilometersats: BigDecimal,
    ): BigDecimal {
        val kostnadKjøring =
            grunnlagForReise.reiseavstandEnVei
                .multiply(BigDecimal.valueOf(2))
                .multiply(kilometersats)

        val sumEkstrakostnader = grunnlagForReise.ekstrakostnader.beregnTotalEkstrakostnadForEnDag()

        return kostnadKjøring + sumEkstrakostnader
    }
}
