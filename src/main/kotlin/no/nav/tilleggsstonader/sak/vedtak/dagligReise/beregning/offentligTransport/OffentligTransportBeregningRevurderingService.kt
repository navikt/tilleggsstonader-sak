package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OffentligTransportBeregningRevurderingService(
    private val vedtakService: VedtakService,
) {
    /**
     * Beholder de reisene og perioder fra forrige iverksatte behandling som er berørt av revurderingen, slik at vi ikke risikerer at gamle
     * vedtak blir reberegnet med et annet resultat, fordi vi eksempelvis har gjort endringer i beregningskoden siden sist.
     */
    fun flettMedForrigeVedtakHvisRevurdering(
        nyttBeregningsresultat: BeregningsresultatOffentligTransport,
        behandling: Saksbehandling,
        beregnFra: LocalDate?,
    ): BeregningsresultatOffentligTransport {
        val forrigeIverksatte =
            hentForrigeIverksatteVedtak(behandling)?.beregningsresultat?.offentligTransport ?: return nyttBeregningsresultat

        brukerfeilHvis(beregnFra == null) { "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen" }

        validerEndringAvAlleredeUtbetaltPeriode(
            nyttBeregningsresultat = nyttBeregningsresultat,
            reiserForrigeBehandling = forrigeIverksatte.reiser,
        )

        return BeregningsresultatOffentligTransport(
            reiser =
                nyttBeregningsresultat.reiser.map { reise ->
                    slåSammenNyeOgGamlePerioder(reise, forrigeIverksatte, beregnFra)
                },
        )
    }

    /**
     * Beholder alle perioder fra forrige vedtak som er starter tidligere enn 30 dager unna [beregnFra]-datoen.
     *
     * Dette gjøres for ikke å reberegne mer enn vi trenger å gjøre, men vi er samtidig nødt til å reberegne enkelte perioder som er f.eks.
     * 25 dager unna [beregnFra]-datoen, ettersom det kan skje at denne perioden etter revurderingen skulle vært en 30-dagersperiode
     * i stedet.
     */
    private fun slåSammenNyeOgGamlePerioder(
        nyBeregningForReise: BeregningsresultatForReise,
        forrigeBeregning: BeregningsresultatOffentligTransport,
        beregnFra: LocalDate,
    ): BeregningsresultatForReise {
        // hvis ikke reisen eksisterer i forrige vedtak, er det bare ny beregning som gjelder
        val reisenIForrigeVedtak =
            forrigeBeregning.reiser.find { it.reiseId == nyBeregningForReise.reiseId }?.perioder
                ?: return nyBeregningForReise

        // Alle perioder som er tidligere enn 30 dager fra endringsdatoen skal kopieres fra tidligere vedtak
        val bevarteGamlePerioder =
            reisenIForrigeVedtak
                .filter { it.grunnlag.fom.plusDays(30L) <= beregnFra }
                .map { it.copy(fraTidligereVedtak = true) }

        val nyeEllerOppdatertePerioder =
            nyBeregningForReise.perioder
                .filter { it.grunnlag.fom.plusDays(30L) > beregnFra }
                .map { nyPeriode ->
                    val tilsvarendePeriodeIForrigeVedtak = reisenIForrigeVedtak.singleOrNull { it.grunnlag == nyPeriode.grunnlag }
                    tilsvarendePeriodeIForrigeVedtak?.copy(fraTidligereVedtak = true) ?: nyPeriode.copy(fraTidligereVedtak = false)
                }

        return nyBeregningForReise.copy(
            perioder = (bevarteGamlePerioder + nyeEllerOppdatertePerioder).sortedBy { it.grunnlag.fom },
        )
    }

    private fun hentForrigeIverksatteVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørDagligReise? =
        behandling.forrigeIverksatteBehandlingId
            ?.let {
                vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(it)
            }?.data
}
