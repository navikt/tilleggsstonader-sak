package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseBeregningRevurderingService(
    private val vedtakRepository: VedtakRepository,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun flettMedForrigeVedtakHvisRevurdering(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        nyttBeregningsresultat: BeregningsresultatOffentligTransport,

        ): BeregningsresultatOffentligTransport {
        val forrigeIverksatte =
            hentForrigeVedtak(behandling)?.beregningsresultat?.offentligTransport ?: return nyttBeregningsresultat

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                behandlingId = behandling.id,
                vedtaksperioder = vedtaksperioder,
            ) ?: brukerfeil("Kan ikke beregne ytelse fordi det ikke er gjort noen endringer i revurderingen")

        validerRevurdering(
            nyttBeregningsresultat = nyttBeregningsresultat,
            reiserForrigeBehandling = forrigeIverksatte.reiser,
        )

        return BeregningsresultatOffentligTransport(
            reiser =
                nyttBeregningsresultat.reiser.map { reise ->
                    slåSammenNyttOgGammeltBeregningsresultat(reise, forrigeIverksatte, tidligsteEndring)
                },
        )
    }

    /**
     * Beholder de periodene fra forrige iverksatte behandling som har fom-dato som ligger mer enn 30 dager unna tidligste endring-datoen.
     *
     * Dette gjøres for ikke å reberegne mer enn vi trenger å gjøre, men vi er samtidig nødt til å reberegne perioder som er f.eks. 29 dager
     * unna tidligste endring-datoen, ettersom denne perioden kan revurderes til å skulle være være en 30-dagersperiode i stedet for.
     */
    private fun slåSammenNyttOgGammeltBeregningsresultat(
        nyBeregningForReise: BeregningsresultatForReise,
        forrigeBeregning: BeregningsresultatOffentligTransport,
        tidligsteEndring: LocalDate,
    ): BeregningsresultatForReise {
        val ANTALL_DAGER_I_REISEPERIODE = 30L
        val perioderSomSkalReberegnes =
            nyBeregningForReise.perioder.filter { it.grunnlag.fom.plusDays(ANTALL_DAGER_I_REISEPERIODE) > tidligsteEndring }
        val beholdFraForrigeVedtak = forrigeBeregning.reiser
            .singleOrNull { it.reiseId == nyBeregningForReise.reiseId }
            ?.perioder
            ?.filter { it.grunnlag.fom.plusDays(ANTALL_DAGER_I_REISEPERIODE) <= tidligsteEndring }
            ?: emptyList()

        return nyBeregningForReise.copy(
            perioder = (beholdFraForrigeVedtak + perioderSomSkalReberegnes).sortedBy { it.grunnlag.fom }
        )
    }

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørDagligReise? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data
}