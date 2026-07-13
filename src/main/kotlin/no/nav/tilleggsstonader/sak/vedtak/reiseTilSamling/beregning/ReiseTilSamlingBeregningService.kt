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
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatForReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBilForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.VedtaksperiodeGrunnlag
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

        val oppfylteOffentligTransport = utgifterTilBeregning.filter { it.fakta is FaktaOffentligTransport }
        val offentligTransport =
            BeregningsresultatOffentligTransport(
                reiser =
                    oppfylteOffentligTransport.map { samling ->
                        samling.fakta as FaktaOffentligTransport
                        BeregningsresultatOffentligTransportForSamling(
                            reiseId = samling.fakta.reiseId,
                            grunnlag =
                                BeregningsgrunnlagOffentligTransportForSamling(
                                    adresse = samling.fakta.adresse,
                                    fom = samling.fom,
                                    tom = samling.tom,
                                    vedtaksperioder =
                                        vedtaksperioder.map { VedtaksperiodeGrunnlag(it) },
                                ),
                            beløp = samling.fakta.utgifterOffentligTransport,
                        )
                    },
            )

        val oppfyltePrivatBil = utgifterTilBeregning.filter { it.fakta is FaktaPrivatBil }
        val privatBil =
            BeregningsresultatPrivatBil(
                reiser =
                    oppfyltePrivatBil.map { samling ->
                        samling.fakta as FaktaPrivatBil
                        BeregningsresultatPrivatBilForSamling(
                            reiseId = samling.fakta.reiseId,
                            grunnlag =
                                BeregningsgrunnlagPrivatBilForSamling(
                                    adresse = samling.fakta.adresse,
                                    fom = samling.fom,
                                    tom = samling.tom,
                                    sats = 2.94.toBigDecimal(),
                                    reiseavstandEnVei = samling.fakta.reiseavstand,
                                    vedtaksperioder =
                                        vedtaksperioder.map { VedtaksperiodeGrunnlag(it) },
                                ),
                            beløp = beregnBelopForPrivatBil(samling.fakta.reiseavstand),
                        )
                    },
            )

        return BeregningReiseTilSamling(
            reiser = listOf(offentligTransport, privatBil),
        )
    }
}

private fun beregnBelopForPrivatBil(reiseavstandEnVei: BigDecimal?): Int? {
    val kilometersats: BigDecimal? = 2.94.toBigDecimal()
    return reiseavstandEnVei
        ?.multiply(BigDecimal.valueOf(2))
        ?.multiply(kilometersats)
        ?.setScale(2, RoundingMode.HALF_UP)
        ?.toInt()
}

private fun validerFinnesSamling(vilkår: List<VilkårReiseTilSamling>) {
    brukerfeilHvis(vilkår.isEmpty()) {
        "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med samling"
    }
}

data class BeregningReiseTilSamling(
    val reiser: List<BeregningsresultatForReiseTilSamling>,
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
            }.firstOrNull { it < 0 }

    feilHvis(ikkePositivUtgift != null) {
        "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
    }
}
