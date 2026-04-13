package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import java.math.BigDecimal
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.allePerioderErSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.finnSnittMellomReiseOgVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilSatsForDelperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatEkstrakostnader
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.springframework.stereotype.Service

// Begrensninger:
// Håndterer ikke ulik kilometersats i årskifte dersom en uke går på tvers av to år.

@Service
class PrivatBilBeregningService(
    private val satsDagligReisePrivatBilProvider: SatsDagligReisePrivatBilProvider,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun beregnRammevedtak(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
    ): RammevedtakPrivatBil? {
        val reiser = mapVilkårTilReiser(oppfylteVilkår)

        val resultatForReiser =
            reiser.mapNotNull { beregnForReise(it, vedtaksperioder) }

        if (resultatForReiser.isEmpty()) return null

        return RammevedtakPrivatBil(reiser = resultatForReiser)
    }

    private fun mapVilkårTilReiser(oppfylteVilkår: List<VilkårDagligReise>): List<ReiseMedPrivatBil> =
        oppfylteVilkår.map { vilkår ->
            val fakta = vilkår.fakta as? FaktaPrivatBil
            brukerfeilHvis(fakta == null) { "Forventet FaktaPrivatBil for daglig reise med privat bil" }
            val aktivitet =
                vilkårperiodeService.hentAktivitet(fakta.aktivitetId)
                    ?: error("Fant ikke aktivitet for aktivitetId=${fakta.aktivitetId}")
            val aktivitetType =
                aktivitet.type as? AktivitetType
                    ?: error("Forventet AktivitetType for aktivitetId=${fakta.aktivitetId}")
            vilkår.tilReiserMedPrivatBil(
                aktivitetType = aktivitetType,
                typeAktivitet = aktivitet.typeAktivitet,
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
                typeAktivitet = reise.typeAktivitet,
                aktivitetType = reise.aktivitetType,
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
                // Justerer delperioder i tilfelle rammevedtaket har blitt kortet ned mot vedtaksperioder
                .beregnSnitt(listOf(Datoperiode(reise.fom, reise.tom)))
                .map { it.first }
                .map { delperiode ->
                    val satser =
                        satsDagligReisePrivatBilProvider
                            .finnAlleSatserInnenforPeriode(delperiode)
                            .map { sats ->
                                val snitt =
                                    delperiode.beregnSnitt(sats)
                                        ?: error(
                                            "Forventer at det skal finnes et snitt mellom delperiode ${delperiode.fom} - ${delperiode.tom} og sats ${sats.fom} - ${sats.tom}",
                                        )
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

                                RammeForReiseMedPrivatBilSatsForDelperiode(
                                    fom = snitt.fom,
                                    tom = snitt.tom,
                                    kilometersats = sats.beløp,
                                    dagsatsUtenParkering = dagsatsUtenParkering,
                                    satsBekreftetVedVedtakstidspunkt = sats.bekreftet,
                                )
                            }

                    RammeForReiseMedPrivatBilDelperiode(
                        fom = delperiode.fom,
                        tom = delperiode.tom,
                        ekstrakostnader =
                            RammeForReiseMedPrivatEkstrakostnader(
                                bompengerPerDag = delperiode.bompengerPerDag,
                                fergekostnadPerDag = delperiode.fergekostnadPerDag,
                            ),
                        reisedagerPerUke = delperiode.reisedagerPerUke,
                        satser = satser.sorted(),
                    )
                }

        return RammeForReiseMedPrivatBilBeregningsgrunnlag(
            fom = reise.fom,
            tom = reise.tom,
            delperioder = delperioder.sortedBy { it.fom },
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
