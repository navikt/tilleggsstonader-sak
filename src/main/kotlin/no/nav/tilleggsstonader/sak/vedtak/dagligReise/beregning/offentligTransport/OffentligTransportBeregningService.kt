package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
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
    private val vilkårperiodeService: VilkårperiodeService,
    private val unleashService: UnleashService,
) {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        oppfylteVilkår: List<VilkårDagligReise>,
        brukersNavKontor: String?,
        behandlingId: BehandlingId,
    ): BeregningsresultatOffentligTransport? {
        val utgifter =
            oppfylteVilkår
                .map { it.tilUtgiftOffentligTransport(behandlingId) }

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
                        // Toggle ON: typeAktivitet fra vilkårets aktivitetstilknytning (ikke-null)
                        // Toggle OFF: fall tilbake til typeAktivitet fra vedtaksperioden (gammelt mønster)
                        typeAktivitet = trettidagerReisePeriode.typeAktivitet ?: vedtaksperiode.typeAktivitet,
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

    private fun VilkårDagligReise.tilUtgiftOffentligTransport(behandlingId: BehandlingId): UtgiftOffentligTransport {
        feilHvis(this.fakta !is FaktaOffentligTransport) {
            "Forventer kun å få inn vilkår med fakta som er av type offentlig transport ved beregning av offentlig transport"
        }

        val typeAktivitet = hentTypeAktivitetForVilkår(this.fakta, behandlingId)

        return UtgiftOffentligTransport(
            reiseId = this.fakta.reiseId,
            fom = this.fom,
            tom = this.tom,
            antallReisedagerPerUke = this.fakta.reisedagerPerUke,
            prisEnkelbillett = this.fakta.prisEnkelbillett,
            prisSyvdagersbillett = this.fakta.prisSyvdagersbillett,
            pris30dagersbillett = this.fakta.prisTrettidagersbillett,
            typeAktivitet = typeAktivitet,
        )
    }

    private fun hentTypeAktivitetForVilkår(
        fakta: FaktaOffentligTransport,
        behandlingId: BehandlingId,
    ): TypeAktivitet? {
        if (!unleashService.isEnabled(Toggle.KAN_KNYTTE_OFFENTLIG_TRANSPORT_TIL_AKTIVITET)) return null

        val aktivitetId =
            fakta.aktivitetId
                ?: feil("Vilkår for offentlig transport mangler aktivitetId. Alle vilkår må knyttes til en aktivitet.")

        val aktivitet =
            vilkårperiodeService.hentAktivitet(aktivitetId, behandlingId)
                ?: feil("Fant ikke aktivitet med id=$aktivitetId for behandling=$behandlingId")

        return aktivitet.typeAktivitet
            ?: feil("Aktivitet med id=$aktivitetId har ikke typeAktivitet satt")
    }
}
