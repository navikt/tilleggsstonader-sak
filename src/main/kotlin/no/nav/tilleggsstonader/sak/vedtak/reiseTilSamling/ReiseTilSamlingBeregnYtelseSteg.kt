package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.OpprettAndelerReiseTilSamlingService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.VedtakReiseTilSamlingRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReiseTilSamlingBeregnYtelseSteg(
    private val beregningService: ReiseTilSamlingBeregningService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    private val opprettAndelerReiseTilSamlingService: OpprettAndelerReiseTilSamlingService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakReiseTilSamlingRequest>(
        stønadstype = listOf(Stønadstype.REISE_TIL_SAMLING_TSO),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: VedtakReiseTilSamlingRequest,
        satsjusteringFra: LocalDate,
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakReiseTilSamlingRequest,
    ) {
        when (vedtak) {
            is InnvilgelseReiseTilSamlingRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseReiseTilSamlingRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder()
        val plan =
            beregningsplanUtleder.utledForInnvilgelse(
                saksbehandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
            )
        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                beregningsplan = plan,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )
        lagreInnvilgetVedtak(
            behandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = vedtak.begrunnelse,
            beregningsplan = plan,
        )
        opprettAndelerReiseTilSamlingService.lagreAndelerForBehandling(saksbehandling)
    }

    private fun lagreInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatReiseTilSamling,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        beregningsplan: Beregningsplan,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseReiseTilSamling(
                        vedtaksperioder = vedtaksperioder,
                        begrunnelse = begrunnelse,
                        beregningsresultat = beregningsresultat,
                        beregningsplan = beregningsplan,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = beregningsplan.legacyTidligsteEndring(),
            ),
        )
    }
}
