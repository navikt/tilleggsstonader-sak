package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingValidering.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingValidering.validerUtgiftHeleVedtaksperioden
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingValidering.validerUtgifterStrekkerSegUtenforVedtaksperiodene
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagPrivatBilForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.VilkårReiseTilSamlingMapper.mapTilVilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ReiseTilSamlingBeregningService(
    private val vilkårService: VilkårService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val satsPrivatBilProvider: SatsPrivatBilProvider,
) {
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        typeVedtak: TypeVedtak,
    ): BeregningReiseTilSamling {
        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )
        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted()

        val oppfylteVilkårReiseTilSamling =
            vilkårService
                .hentOppfylteReiseTilSamlingVilkår(
                    behandling.id,
                ).map { it.mapTilVilkårReiseTilSamling() }
                .sortedBy { it.fom }

        val utgifterTilBeregning =
            oppfylteVilkårReiseTilSamling.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(
                vedtaksperioderBeregning,
            )

        validerUtgifter(
            utgifter = utgifterTilBeregning,
            vedtakstype = typeVedtak,
            vedtaksperioder = vedtaksperioder,
        )
        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifterTilBeregning)

        validerUtgifterStrekkerSegUtenforVedtaksperiodene(
            utgifterTilBeregning,
            vedtaksperioderBeregning,
        )
        validerFinnesSamling(utgifterTilBeregning)
        val offentligTransport =
            beregnOffentligTransport(
                utgifterTilBeregning,
                vedtaksperioder,
            )

        val privatBil =
            beregnPrivatBil(
                utgifterTilBeregning,
                vedtaksperioder,
            )
        return BeregningReiseTilSamling(
            offentligTransport = offentligTransport,
            privatBil = privatBil,
        )
    }

    private fun beregnOffentligTransport(
        utgifter: List<VilkårReiseTilSamling>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<BeregningsresultatOffentligTransport> {
        val oppfylteOffentligTransport =
            utgifter.filter { it.fakta is FaktaOffentligTransport }

        return oppfylteOffentligTransport.map { samling ->
            val fakta = samling.fakta as FaktaOffentligTransport

            BeregningsresultatOffentligTransport(
                reiseId = fakta.reiseId,
                grunnlag =
                    BeregningsgrunnlagOffentligTransportForSamling(
                        adresse = fakta.adresse,
                        fom = samling.fom,
                        tom = samling.tom,
                        vedtaksperioder =
                            vedtaksperioder
                                .filter { it.overlapper(samling) }
                                .map(::VedtaksperiodeGrunnlag),
                    ),
                beløp = fakta.utgifterOffentligTransport,
            )
        }
    }

    private fun beregnPrivatBil(
        utgifter: List<VilkårReiseTilSamling>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<BeregningsresultatPrivatBil> {
        val oppfyltePrivatBil =
            utgifter.filter { it.fakta is FaktaPrivatBil }

        return oppfyltePrivatBil.map { samling ->
            val fakta = samling.fakta as FaktaPrivatBil

            val sats =
                satsPrivatBilProvider
                    .finnRelevantKilometerSatsForPeriode(samling)

            val grunnlag =
                BeregningsgrunnlagPrivatBilForSamling(
                    adresse = fakta.adresse,
                    fom = samling.fom,
                    tom = samling.tom,
                    sats = sats.beløp,
                    totaltReiseavstand = fakta.reiseavstand,
                    vedtaksperioder =
                        vedtaksperioder
                            .filter { it.overlapper(samling) }
                            .map(::VedtaksperiodeGrunnlag),
                )

            BeregningsresultatPrivatBil(
                reiseId = fakta.reiseId,
                grunnlag = grunnlag,
                beløp = beregnBelopForPrivatBil(grunnlag),
            )
        }
    }

    private fun beregnBelopForPrivatBil(grunnlag: BeregningsgrunnlagPrivatBilForSamling): BigDecimal =
        grunnlag.totaltReiseavstand.multiply(grunnlag.sats).setScale(0, RoundingMode.HALF_UP)
}

private fun validerFinnesSamling(vilkår: List<VilkårReiseTilSamling>) {
    brukerfeilHvis(vilkår.isEmpty()) {
        "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med samling"
    }
}

data class BeregningReiseTilSamling(
    val offentligTransport: List<BeregningsresultatOffentligTransport>,
    val privatBil: List<BeregningsresultatPrivatBil>,
)

fun validerUtgifter(
    utgifter: List<VilkårReiseTilSamling>,
    vedtakstype: TypeVedtak,
    vedtaksperioder: List<Vedtaksperiode>,
) {
    // Tillat opphør av hele saken
    if (vedtakstype == TypeVedtak.OPPHØR && utgifter.isEmpty() && vedtaksperioder.isEmpty()) return

    brukerfeilHvis(utgifter.isEmpty()) {
        "Det er ikke lagt inn noen oppfylte utgiftsperioder"
    }

    feilHvis(utgifter.overlapper()) {
        "Utgiftsperioder overlapper"
    }

    val ikkePositivUtgift =
        utgifter
            .mapNotNull {
                (it.fakta as? FaktaOffentligTransport)
                    ?.utgifterOffentligTransport
            }.firstOrNull { it < 0.toBigDecimal() }

    feilHvis(ikkePositivUtgift != null) {
        "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
    }
}
