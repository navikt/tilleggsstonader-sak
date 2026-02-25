package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.allePerioderErSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.finnesPerioderSomOverlapper
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.finnSnittMellomReiseOgVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Ekstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.SatsForPeriodePrivatBil
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
        val reiseOgVedtaksperioderSnitt = finnSnittMellomReiseOgVedtaksperioder(reise, vedtaksperioder)

        return reiseOgVedtaksperioderSnitt.justertReiseperiode?.let { justertReise ->
            validerVedtaksperioderErSammenhengendeInnenforReise(justertReise, reiseOgVedtaksperioderSnitt.justerteVedtaksperioder)
            RammeForReiseMedPrivatBil(
                reiseId = reise.reiseId,
                aktivitetsadresse = reise.aktivitetsadresse,
                grunnlag = lagBeregningsgrunnlagForReise(justertReise, reiseOgVedtaksperioderSnitt.justerteVedtaksperioder),
            )
        }
    }

    private fun validerVedtaksperioderErSammenhengendeInnenforReise(
        justertReise: ReiseMedPrivatBil,
        justerteVedtaksperioder: List<Vedtaksperiode>,
    ) {
        require(justertReise.fom == justerteVedtaksperioder.minOf { it.fom }) {
            "Fom på reise er ulik tidligste fom på vedtaksperiodene"
        }
        require(justertReise.tom == justerteVedtaksperioder.maxOf { it.tom }) {
            "Tom på reise ulik største tom på vedtaksperiodene"
        }
        require(!justerteVedtaksperioder.finnesPerioderSomOverlapper()) {
            "Vedtaksperioder innenfor en reise kan ikke overlappe"
        }
        require(justerteVedtaksperioder.allePerioderErSammenhengende()) {
            "Alle vedtaksperioder må være sammenhengende"
        }
    }

    private fun lagBeregningsgrunnlagForReise(
        reise: ReiseMedPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsgrunnlagForReiseMedPrivatBil {
        val ekstrakostnader =
            Ekstrakostnader(
                fergekostnadEnVei = reise.fergekostandEnVei,
                bompengerEnVei = reise.bompengerEnVei,
            )
        return BeregningsgrunnlagForReiseMedPrivatBil(
            fom = reise.fom,
            tom = reise.tom,
            reisedagerPerUke = reise.reisedagerPerUke,
            reiseavstandEnVei = reise.reiseavstandEnVei,
            ekstrakostnader = ekstrakostnader,
            satser = beregnSatserForReise(reise, ekstrakostnader),
            vedtaksperioder = vedtaksperioder,
        )
    }

    private fun beregnSatserForReise(
        reise: ReiseMedPrivatBil,
        ekstrakostnader: Ekstrakostnader,
    ): List<SatsForPeriodePrivatBil> =
        reise.splitPerÅr { fom, tom ->
            val periode = Datoperiode(fom, tom)
            val sats = finnRelevantKilometerSats(periode)
            SatsForPeriodePrivatBil(
                fom = fom,
                tom = tom,
                satsBekreftetVedVedtakstidspunkt = sats.bekreftet,
                kilometersats = sats.beløp,
                dagsatsUtenParkering =
                    beregnDagsatsUtenParkering(
                        reiseavstandEnVei = reise.reiseavstandEnVei,
                        ekstrakostnader = ekstrakostnader,
                        kilometersats = sats.beløp,
                    ),
            )
        }

    private fun beregnDagsatsUtenParkering(
        reiseavstandEnVei: BigDecimal,
        ekstrakostnader: Ekstrakostnader,
        kilometersats: BigDecimal,
    ): BigDecimal {
        val kostnadKjøring =
            reiseavstandEnVei
                .multiply(BigDecimal.valueOf(2))
                .multiply(kilometersats)

        val sumEkstrakostnader = ekstrakostnader.beregnTotalEkstrakostnadForEnDag()

        return kostnadKjøring + sumEkstrakostnader
    }
}
