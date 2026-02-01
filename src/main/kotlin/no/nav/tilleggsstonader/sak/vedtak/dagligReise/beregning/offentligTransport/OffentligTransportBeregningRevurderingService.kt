package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OffentligTransportBeregningRevurderingService(
    private val vedtakRepository: VedtakRepository,
) {
    /**
     * Beholder de reisene og perioder fra forrige iverksatte behandling som er berørt av revurderingen, slik at vi ikke risikerer at gamle
     * vedtak blir reberegnet med et annet resultat, fordi vi eksempelvis har gjort endringer i beregningskoden siden sist.
     */
    fun flettMedForrigeVedtakHvisRevurdering(
        nyttBeregningsresultat: BeregningsresultatOffentligTransport,
        behandling: Saksbehandling,
        tidligsteEndring: LocalDate?,
    ): BeregningsresultatOffentligTransport {
        val forrigeIverksatte =
            hentForrigeVedtak(behandling)?.beregningsresultat?.offentligTransport ?: return nyttBeregningsresultat

        brukerfeilHvis(tidligsteEndring == null) { "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen" }

        validerEndringAvAlleredeUtbetaltPeriode(
            nyttBeregningsresultat = nyttBeregningsresultat,
            reiserForrigeBehandling = forrigeIverksatte.reiser,
        )

        return BeregningsresultatOffentligTransport(
            reiser =
                nyttBeregningsresultat.reiser.map { reise ->
                    slåSammenNyeOgGamlePerioder(reise, forrigeIverksatte, tidligsteEndring)
                },
        )
    }

    /**
     * Beholder alle perioder som er eldre enn 30 dager unna tidligste endring-datoen. Reiser som kke har blitt endret på i det hele
     * tatt av saksbehandler, beholdes i sin helhet fra forrige iverksatte vedtak.
     *
     * Dette gjøres for ikke å reberegne mer enn vi trenger å gjøre, men vi er samtidig nødt til å reberegne enkelte perioder som er f.eks.
     * 25 dager unna tidligste endring-datoen, ettersom det kan skje at denne perioden etter revurderingen skulle vært en 30-dagersperiode
     * i stedet.
     */
    private fun slåSammenNyeOgGamlePerioder(
        nyBeregningForReise: BeregningsresultatForReise,
        forrigeBeregning: BeregningsresultatOffentligTransport,
        tidligsteEndring: LocalDate,
    ): BeregningsresultatForReise {
        // hvis ikke reisen eksisterer i forrige vedtak, er det bare ny beregning som gjelder
        val reisenIForrigeVedtak =
            forrigeBeregning.reiser.find { it.reiseId == nyBeregningForReise.reiseId }?.perioder
                ?: return nyBeregningForReise

        // Alle perioder som er tidligere enn 30 dager fra endringsdatoen skal kopieres fra tidligere vedtak
        val bevarteGamlePerioder =
            reisenIForrigeVedtak
                .filter { it.grunnlag.fom.plusDays(30L) <= tidligsteEndring }
                .map { it.copy(fraTidligereVedtak = true) }

        val nyeEllerOppdatertePerioder =
            nyBeregningForReise.perioder
                .filter { it.grunnlag.fom.plusDays(30L) > tidligsteEndring }
                .map { nyPeriode ->
                    val tilsvarendePeriodeIForrigeVedtak = reisenIForrigeVedtak.singleOrNull { it.grunnlag == nyPeriode.grunnlag }
                    tilsvarendePeriodeIForrigeVedtak?.copy(fraTidligereVedtak = true) ?: nyPeriode.copy(fraTidligereVedtak = false)
                }

        return nyBeregningForReise.copy(
            perioder = (bevarteGamlePerioder + nyeEllerOppdatertePerioder).sortedBy { it.grunnlag.fom },
        )
    }

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørDagligReise? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data
}
