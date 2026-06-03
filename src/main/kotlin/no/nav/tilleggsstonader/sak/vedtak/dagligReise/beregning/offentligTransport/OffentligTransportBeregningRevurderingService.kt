package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OffentligTransportBeregningRevurderingService {
    /**
     * Beholder de reisene og perioder fra forrige iverksatte behandling som er berørt av revurderingen, slik at vi ikke risikerer at gamle
     * vedtak blir reberegnet med et annet resultat, fordi vi eksempelvis har gjort endringer i beregningskoden siden sist.
     */
    fun flettMedForrigeVedtakHvisRevurdering(
        nyttBeregningsresultat: BeregningsresultatOffentligTransport,
        forrigeOffentligTransport: BeregningsresultatOffentligTransport?,
        beregnFra: LocalDate?,
    ): BeregningsresultatOffentligTransport {
        val forrigeIverksatte = forrigeOffentligTransport ?: return nyttBeregningsresultat

        feilHvis(beregnFra == null) { "Vi mangler beregnFra-dato for å flette sammen nytt og gammelt vedtak." }

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
     * Beholder alle perioder fra forrige vedtak som starter før [beregnFra]-datoen.
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

        // Alle perioder som er tidligere beregn fra-datoen skal kopieres fra tidligere vedtak
        val bevarteGamlePerioder =
            reisenIForrigeVedtak
                .filter { it.grunnlag.fom < beregnFra }
                .map { it.copy(fraTidligereVedtak = true) }

        // Perioder som har identisk grunnlag som forrige vedtak skal ikke reberegnes
        val nyeEllerOppdatertePerioder =
            nyBeregningForReise.perioder
                .filter { it.grunnlag.fom >= beregnFra }
                .map { nyPeriode ->
                    val tilsvarendePeriodeIForrigeVedtak = reisenIForrigeVedtak.singleOrNull { it.grunnlag == nyPeriode.grunnlag }
                    tilsvarendePeriodeIForrigeVedtak?.copy(fraTidligereVedtak = true) ?: nyPeriode.copy(fraTidligereVedtak = false)
                }

        return nyBeregningForReise.copy(
            perioder = (bevarteGamlePerioder + nyeEllerOppdatertePerioder).sortedBy { it.grunnlag.fom },
        )
    }
}
