package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise

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
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning.OpprettAndelerReiseOppstartAvslutningHjemreiseService
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning.ReiseOppstartAvslutningHjemreiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.InnvilgelseReiseOppstartAvslutningHjemreiseRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto.VedtakReiseOppstartAvslutningHjemreiseRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReiseOppstartAvslutningHjemreiseBeregnYtelseSteg(
    private val beregningService: ReiseOppstartAvslutningHjemreiseBeregningService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    private val opprettAndelerReiseOppstartAvslutningHjemreiseService: OpprettAndelerReiseOppstartAvslutningHjemreiseService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakReiseOppstartAvslutningHjemreiseRequest>(
        stønadstype =
            listOf(
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
            ),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: VedtakReiseOppstartAvslutningHjemreiseRequest,
        satsjusteringFra: LocalDate,
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakReiseOppstartAvslutningHjemreiseRequest,
    ) {
        when (vedtak) {
            is InnvilgelseReiseOppstartAvslutningHjemreiseRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseReiseOppstartAvslutningHjemreiseRequest,
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

        opprettAndelerReiseOppstartAvslutningHjemreiseService.lagreAndelerForBehandling(saksbehandling)
    }

    private fun lagreInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningReiseOppstartAvslutningHjemreise,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        beregningsplan: Beregningsplan,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseReiseOppstartAvslutningHjemreise(
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
