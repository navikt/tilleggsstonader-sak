package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.allePerioderErSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnittMotListe
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.finnSnittMellomReiseOgVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatEkstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service
import java.math.BigDecimal

// Begrensninger:
// Håndterer ikke ulik kilometersats i årskifte dersom en uke går på tvers av to år.

@Service
class PrivatBilBeregningService(
    private val satsDagligReisePrivatBilProvider: SatsDagligReisePrivatBilProvider,
) {
    fun beregnRammevedtak(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
    ): RammevedtakPrivatBil? {
        // TODO - skrive om denne til å ha delperioder og bruke map
        val reiseInformasjon = oppfylteVilkår.map { it.tilReiserMedPrivatBil() }
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
            validerVedtaksperioderErSammenhengendeInnenforReise(
                justertReise,
                reiseOgVedtaksperioderSnitt.justerteVedtaksperioder,
            )
            RammeForReiseMedPrivatBil(
                reiseId = reise.reiseId,
                aktivitetsadresse = reise.aktivitetsadresse,
                grunnlag =
                    lagBeregningsgrunnlagForReise(
                        justertReise,
                        reiseOgVedtaksperioderSnitt.justerteVedtaksperioder,
                    ),
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

        require(!justerteVedtaksperioder.overlapper()) {
            "Vedtaksperioder innenfor en reise kan ikke overlappe"
        }

        brukerfeilHvis(!justerteVedtaksperioder.allePerioderErSammenhengende()) {
            "Alle vedtaksperioder må være sammenhengende innenfor en reise"
        }
    }

    private fun lagBeregningsgrunnlagForReise(
        reise: ReiseMedPrivatBil,
        vedtaksperioder: List<Vedtaksperiode>,
    ): RammeForReiseMedPrivatBilBeregningsgrunnlag {
        val delperioder =
            reise.delPerioder
                .beregnSnittMotListe(satsDagligReisePrivatBilProvider.alleSatser)
                .map { (delperiode, sats) ->
                    val dagsatsUtenParkering =
                        beregnDagsatsUtenParkering(
                            reiseavstandEnVei = reise.reiseavstandEnVei,
                            ekstrakostnader =
                                RammeForReiseMedPrivatEkstrakostnader(
                                    bompengerPerDag = delperiode.bompengerPerDag,
                                    fergekostnadPerDag = delperiode.fergekostnadPerDag,
                                ),
                            kilometersats = sats.beløp,
                        )
                    RammeForReiseMedPrivatBilDelperiode(
                        fom = delperiode.fom,
                        tom = delperiode.tom,
                        ekstrakostnader =
                            RammeForReiseMedPrivatEkstrakostnader(
                                bompengerPerDag = delperiode.bompengerPerDag,
                                fergekostnadPerDag = delperiode.fergekostnadPerDag,
                            ),
                        reisedagerPerUke = delperiode.reisedagerPerUke,
                        satsBekreftetVedVedtakstidspunkt = sats.bekreftet,
                        kilometersats = sats.beløp,
                        dagsatsUtenParkering = dagsatsUtenParkering,
                    )
                }

        return RammeForReiseMedPrivatBilBeregningsgrunnlag(
            fom = reise.fom,
            tom = reise.tom,
            delPerioder = delperioder.sortedBy { it.fom },
            reiseavstandEnVei = reise.reiseavstandEnVei,
            vedtaksperioder = vedtaksperioder,
        )
    }

    private fun beregnDagsatsUtenParkering(
        reiseavstandEnVei: BigDecimal,
        ekstrakostnader: RammeForReiseMedPrivatEkstrakostnader,
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
