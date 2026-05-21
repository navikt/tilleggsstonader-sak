package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.finnSnittMellomReiseOgVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service

@Service
class OffentligTransportBeregningService(
    private val offentligTransportBeregningRevurderingService: OffentligTransportBeregningRevurderingService,
) {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkårDagligReise: List<VilkårDagligReise>,
        forrigeBeregningsresultat: BeregningsresultatOffentligTransport?,
        brukersNavKontor: String?,
        beregningsplan: Beregningsplan,
    ): BeregningsresultatOffentligTransport? {
        if (beregningsplan.omfang == Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT) {
            return forrigeBeregningsresultat
                ?: feil("Kan ikke gjenbruke forrige beregningsresultat uten forrige iverksatt behandling")
        }

        val oppfylteVilkårOffentligTransport = oppfylteVilkårDagligReise.filter { it.fakta is FaktaOffentligTransport }
        if (oppfylteVilkårOffentligTransport.isEmpty()) return null

        val nyttBeregningsresultat =
            beregn(
                vedtaksperioder = vedtaksperioder,
                oppfylteVilkår = oppfylteVilkårOffentligTransport,
                brukersNavKontor = brukersNavKontor,
            ) ?: return null

        return offentligTransportBeregningRevurderingService
            .flettMedForrigeVedtakHvisRevurdering(
                nyttBeregningsresultat = nyttBeregningsresultat,
                forrigeOffentligTransport = forrigeBeregningsresultat,
                beregnFra = beregningsplan.beregnFra(),
            ).sorterReiserOgPerioder()
    }

    private fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
        brukersNavKontor: String?,
    ): BeregningsresultatOffentligTransport? {
        val utgifter =
            oppfylteVilkår
                .map { it.tilUtgiftOffentligTransport() }

        val resultatForReiser =
            utgifter.mapNotNull { reise ->
                beregnForReise(reise, vedtaksperioder, brukersNavKontor)
            }

        if (resultatForReiser.isEmpty()) return null

        return BeregningsresultatOffentligTransport(
            reiser = resultatForReiser,
        )
    }

    private fun beregnForReise(
        reise: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
        brukersNavKontor: String?,
    ): BeregningsresultatForReise? {
        val (justerteVedtaksperioder, justertReiseperiode) =
            finnSnittMellomReiseOgVedtaksperioder(
                reise,
                vedtaksperioder,
            )

        if (justertReiseperiode == null) return null

        val trettidagerReisePerioder = justertReiseperiode.delTil30Dagersperioder()

        return BeregningsresultatForReise(
            reiseId = reise.reiseId,
            perioder =
                trettidagerReisePerioder.map { trettidagerReiseperiode ->
                    beregnForTrettiDagersPeriode(trettidagerReiseperiode, justerteVedtaksperioder, brukersNavKontor)
                },
        )
    }

    private fun beregnForTrettiDagersPeriode(
        trettidagerReisePeriode: UtgiftOffentligTransport,
        vedtaksperioder: List<Vedtaksperiode>,
        brukersNavKontor: String?,
    ): BeregningsresultatForPeriode {
        val vedtaksperiodeGrunnlag =
            finnSnittMellomReiseOgVedtaksperioder(trettidagerReisePeriode, vedtaksperioder)
                .justerteVedtaksperioder
                .map { vedtaksperiode ->
                    VedtaksperiodeGrunnlag(
                        vedtaksperiode = vedtaksperiode,
                        antallReisedager =
                            finnReisedagerIPeriode(
                                vedtaksperiode,
                                trettidagerReisePeriode.antallReisedagerPerUke,
                            ),
                    )
                }

        val grunnlag =
            BeregningsgrunnlagOffentligTransport(
                fom = trettidagerReisePeriode.fom,
                tom = trettidagerReisePeriode.tom,
                antallReisedagerPerUke = trettidagerReisePeriode.antallReisedagerPerUke,
                prisEnkeltbillett = trettidagerReisePeriode.prisEnkelbillett,
                prisSyvdagersbillett = trettidagerReisePeriode.prisSyvdagersbillett,
                pris30dagersbillett = trettidagerReisePeriode.pris30dagersbillett,
                antallReisedager = vedtaksperiodeGrunnlag.sumOf { it.antallReisedagerIVedtaksperioden },
                vedtaksperioder = vedtaksperiodeGrunnlag,
                brukersNavKontor = brukersNavKontor,
            )

        return BeregningsresultatForPeriode(
            grunnlag = grunnlag,
            beløp = finnBilligsteAlternativForTrettidagersPeriode(grunnlag).billigsteBelop,
            billettdetaljer = finnBilligsteAlternativForTrettidagersPeriode(grunnlag).billettyper,
        )
    }

    private fun VilkårDagligReise.tilUtgiftOffentligTransport(): UtgiftOffentligTransport {
        feilHvis(this.fakta !is FaktaOffentligTransport) {
            "Forventer kun å få inn vilkår med fakta som er av type offentlig transport ved beregning av offentlig transport"
        }

        return UtgiftOffentligTransport(
            reiseId = this.fakta.reiseId,
            fom = this.fom,
            tom = this.tom,
            antallReisedagerPerUke = this.fakta.reisedagerPerUke,
            prisEnkelbillett = this.fakta.prisEnkelbillett,
            prisSyvdagersbillett = this.fakta.prisSyvdagersbillett,
            pris30dagersbillett = this.fakta.prisTrettidagersbillett,
        )
    }
}
