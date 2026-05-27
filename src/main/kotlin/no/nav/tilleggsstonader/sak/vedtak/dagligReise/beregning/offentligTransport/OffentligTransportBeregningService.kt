package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.norskFormat
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service

@Service
class OffentligTransportBeregningService(
    private val offentligTransportBeregningRevurderingService: OffentligTransportBeregningRevurderingService,
    private val vilkårperiodeService: VilkårperiodeService,
) {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkårDagligReise: List<VilkårDagligReise>,
        forrigeBeregningsresultat: BeregningsresultatOffentligTransport?,
        brukersNavKontor: String?,
        beregningsplan: Beregningsplan,
        behandling: Saksbehandling,
    ): BeregningsresultatOffentligTransport? {
        if (beregningsplan.omfang == Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT) {
            return forrigeBeregningsresultat
                ?: feil("Kan ikke gjenbruke forrige beregningsresultat uten forrige iverksatt behandling")
        }

        if (behandling.stønadstype == Stønadstype.DAGLIG_REISE_TSR) {
            validerTypeAktivitetForOffentligTransportVilkår(oppfylteVilkårDagligReise, behandling.id)
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
            typeAktivitet = reise.typeAktivitet,
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
            typeAktivitet = this.fakta.typeAktivitet,
        )
    }

    private fun validerTypeAktivitetForOffentligTransportVilkår(
        vilkår: List<VilkårDagligReise>,
        behandlingId: BehandlingId,
    ) {
        val offentligTransportVilkår = vilkår.filter { it.fakta is FaktaOffentligTransport }

        offentligTransportVilkår.forEach { vilkår ->
            val fakta = vilkår.fakta as FaktaOffentligTransport
            brukerfeilHvis(fakta.typeAktivitet == null) {
                "Alle reiser med offentlig transport må ha typeAktivitet satt. " +
                    "Reise mellom ${vilkår.fom.norskFormat()} - ${vilkår.tom.norskFormat()} mangler typeAktivitet."
            }

            val vilkåretsTypeAktivitet = fakta.typeAktivitet
            vilkårperiodeService.validerAktivitetMedTypeAktivitetInnenforPeriode(
                typeAktivitet = vilkåretsTypeAktivitet,
                periode = Datoperiode(fom = vilkår.fom, tom = vilkår.tom),
                behandlingId = behandlingId,
            )
        }
    }
}
