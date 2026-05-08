package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.tidligsteendring.ForenkletMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class OffentligTransportBeregningRevurderingService(
    private val vedtakService: VedtakService,
    private val vilkårperiodeService: VilkårperiodeService,
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
        val forrigeIverksatteBehandlingId = behandling.forrigeIverksatteBehandlingId ?: return nyttBeregningsresultat
        val forrigeIverksatte =
            hentForrigeIverksatteVedtak(forrigeIverksatteBehandlingId)?.beregningsresultat?.offentligTransport
                ?: return nyttBeregningsresultat

        brukerfeilHvis(beregnFra == null) { "Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen" }

        validerEndringAvAlleredeUtbetaltPeriode(
            nyttBeregningsresultat = nyttBeregningsresultat,
            reiserForrigeBehandling = forrigeIverksatte.reiser,
        )

        val nåværendeMålgrupper = vilkårperiodeService.hentVilkårperioder(behandling.id).målgrupper
        val tidligereMålgrupper = vilkårperiodeService.hentVilkårperioder(forrigeIverksatteBehandlingId).målgrupper

        return BeregningsresultatOffentligTransport(
            reiser =
                nyttBeregningsresultat.reiser.map { reise ->
                    slåSammenNyeOgGamlePerioder(
                        nyBeregningForReise = reise,
                        forrigeBeregning = forrigeIverksatte,
                        beregnFra = beregnFra,
                        nåværendeMålgrupper = nåværendeMålgrupper,
                        tidligereMålgrupper = tidligereMålgrupper,
                    )
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
        nåværendeMålgrupper: List<VilkårperiodeMålgruppe>,
        tidligereMålgrupper: List<VilkårperiodeMålgruppe>,
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
                    val tilsvarendePeriodeIForrigeVedtak =
                        reisenIForrigeVedtak.singleOrNull { it.grunnlag == nyPeriode.grunnlag }
                    if (tilsvarendePeriodeIForrigeVedtak != null &&
                        !erMålgruppeTypeEndretForPeriode(nyPeriode.grunnlag, nåværendeMålgrupper, tidligereMålgrupper)
                    ) {
                        tilsvarendePeriodeIForrigeVedtak.copy(fraTidligereVedtak = true)
                    } else {
                        nyPeriode.copy(fraTidligereVedtak = false)
                    }
                }

        return nyBeregningForReise.copy(
            perioder = (bevarteGamlePerioder + nyeEllerOppdatertePerioder).sortedBy { it.grunnlag.fom },
        )
    }

    /**
     * Sjekker om MålgruppeType-dekning er endret for perioden – enten ved at en type er fjernet
     * eller at datointervallet til en type har endret seg innenfor perioden.
     * Brukes for å fange opp endringer der MålgruppeType endres (f.eks. DAGPENGER → TILTAKSPENGER) selv om begge
     * mapper til samme FaktiskMålgruppe (ARBEIDSSØKER), slik at slike perioder vises i beregningsresultet og brevet dersom målgruppe er endret.
     * Rene tillegg (ny type lagt til uten at eksisterende fjernes eller endres) regnes ikke som en endring.
     */
    private fun erMålgruppeTypeEndretForPeriode(
        periode: Periode<LocalDate>,
        nåværendeMålgrupper: List<VilkårperiodeMålgruppe>,
        tidligereMålgrupper: List<VilkårperiodeMålgruppe>,
    ): Boolean {
        fun aktiveDekninger(målgrupper: List<VilkårperiodeMålgruppe>): Set<ForenkletMålgruppe> =
            målgrupper
                .filter { it.status != Vilkårstatus.SLETTET && it.resultat == ResultatVilkårperiode.OPPFYLT }
                .filter { it.overlapper(periode) }
                .map {
                    ForenkletMålgruppe(
                        type = it.type as MålgruppeType,
                        fom = maxOf(it.fom, periode.fom),
                        tom = minOf(it.tom, periode.tom),
                        resultat = ResultatVilkårperiode.OPPFYLT,
                    )
                }.toSet()

        val tidligereDekninger = aktiveDekninger(tidligereMålgrupper)
        val nåværendeDekninger = aktiveDekninger(nåværendeMålgrupper)
        return !nåværendeDekninger.containsAll(tidligereDekninger)
    }

    private fun hentForrigeIverksatteVedtak(behandlingId: BehandlingId): InnvilgelseEllerOpphørDagligReise? =
        vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(behandlingId)?.data
}
