package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning.ReiseOppstartAvslutningHjemreiseValidering.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning.ReiseOppstartAvslutningHjemreiseValidering.validerUtgiftHeleVedtaksperioden
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning.ReiseOppstartAvslutningHjemreiseValidering.validerUtgifterStrekkerSegUtenforVedtaksperiodene
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsgrunnlagPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.VilkårReiseOppstartAvslutningHjemreiseMapper.mapTilVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.VilkårReiseOppstartAvslutningHjemreise
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ReiseOppstartAvslutningHjemreiseBeregningService(
    private val vilkårService: VilkårService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val satsPrivatBilProvider: SatsPrivatBilProvider,
) {
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        typeVedtak: TypeVedtak,
        beregningsplan: Beregningsplan,
    ): BeregningReiseOppstartAvslutningHjemreise {
        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )
        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted()

        val oppfylteVilkårReiseOppstartAvslutningHjemreise =
            vilkårService
                .hentOppfylteReiseOppstartAvslutningHjemreiseVilkår(
                    behandling.id,
                ).map { it.mapTilVilkårReiseOppstartAvslutningHjemreise() }
                .sortedBy { it.fom }

        val utgifterTilBeregning =
            oppfylteVilkårReiseOppstartAvslutningHjemreise.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(
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
        validerFinnesReise(utgifterTilBeregning)
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
        return BeregningReiseOppstartAvslutningHjemreise(
            offentligTransport = offentligTransport,
            privatBil = privatBil,
        )
    }

    private fun beregnOffentligTransport(
        utgifter: List<VilkårReiseOppstartAvslutningHjemreise>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<BeregningsresultatOffentligTransport> {
        val oppfylteOffentligTransport =
            utgifter.filter { it.fakta is FaktaOffentligTransport }

        return oppfylteOffentligTransport.map { reise ->
            val fakta = reise.fakta as FaktaOffentligTransport

            BeregningsresultatOffentligTransport(
                reiseId = fakta.reiseId,
                aktivitetId = fakta.aktivitetId,
                grunnlag =
                    BeregningsgrunnlagOffentligTransport(
                        adresse = fakta.adresse,
                        fom = reise.fom,
                        tom = reise.tom,
                        vedtaksperioder =
                            vedtaksperioder
                                .filter { it.overlapper(reise) }
                                .map(::VedtaksperiodeGrunnlag),
                    ),
                beløp = fakta.utgifterOffentligTransport,
            )
        }
    }

    private fun beregnPrivatBil(
        utgifter: List<VilkårReiseOppstartAvslutningHjemreise>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): List<BeregningsresultatPrivatBil> {
        val oppfyltePrivatBil =
            utgifter.filter { it.fakta is FaktaPrivatBil }

        return oppfyltePrivatBil.map { reise ->
            val fakta = reise.fakta as FaktaPrivatBil

            val sats =
                satsPrivatBilProvider
                    .finnRelevantKilometerSatsForPeriode(reise)

            val grunnlag =
                BeregningsgrunnlagPrivatBil(
                    adresse = fakta.adresse,
                    fom = reise.fom,
                    tom = reise.tom,
                    sats = sats.beløp,
                    totaltReiseavstand = fakta.reiseavstand,
                    vedtaksperioder =
                        vedtaksperioder
                            .filter { it.overlapper(reise) }
                            .map(::VedtaksperiodeGrunnlag),
                )

            BeregningsresultatPrivatBil(
                reiseId = fakta.reiseId,
                aktivitetId = fakta.aktivitetId,
                grunnlag = grunnlag,
                beløp = beregnBelopForPrivatBil(grunnlag),
            )
        }
    }

    private fun beregnBelopForPrivatBil(grunnlag: BeregningsgrunnlagPrivatBil): BigDecimal =
        grunnlag.totaltReiseavstand.multiply(grunnlag.sats).setScale(0, RoundingMode.HALF_UP)
}

private fun validerFinnesReise(vilkår: List<VilkårReiseOppstartAvslutningHjemreise>) {
    brukerfeilHvis(vilkår.isEmpty()) {
        "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise"
    }
}

fun validerUtgifter(
    utgifter: List<VilkårReiseOppstartAvslutningHjemreise>,
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
