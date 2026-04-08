package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.OpprettAndelerDagligReiseService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseBeregnYtelseSteg(
    private val beregningService: DagligReiseBeregningService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    private val opphørValideringService: OpphørValideringService,
    private val dagligReiseVedtakService: DagligReiseVedtakService,
    private val opprettAndelerDagligReiseService: OpprettAndelerDagligReiseService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakDagligReiseRequest>(
        stønadstype = listOf(Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
    /**
     * En daglige reiser behandling skal kun havne i steg "KJØRELISTE" dersom forrige behandling
     * inneholdt et rammevedtak for privat bil.
     */
    override fun nesteSteg(
        saksbehandling: Saksbehandling,
        kanBehandlePrivatBil: Boolean,
    ): StegType {
        if (dagligReiseVedtakService.forrigeIverksatteBehandlingHarRammevedtakForPrivatBil(saksbehandling.forrigeIverksatteBehandlingId)) {
            return StegType.KJØRELISTE
        }

        return super.nesteSteg(saksbehandling, kanBehandlePrivatBil)
    }

    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: VedtakDagligReiseRequest,
        satsjusteringFra: LocalDate,
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakDagligReiseRequest,
    ) {
        when (vedtak) {
            is InnvilgelseDagligReiseRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
            is AvslagDagligReiseDto -> dagligReiseVedtakService.lagreAvslag(saksbehandling, vedtak)
            is OpphørDagligReiseRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseDagligReiseRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder()
        val plan = beregningsplanUtleder.utledForInnvilgelse(saksbehandling, vedtaksperioder)
        val (beregningsresultat, rammevedtakPrivatBil) =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                beregningsplan = plan,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )
        dagligReiseVedtakService.lagreInnvilgetVedtak(
            behandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            rammevedtakPrivatBil = rammevedtakPrivatBil,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = vedtak.begrunnelse,
            beregningsplan = plan,
        )

        opprettAndelerDagligReiseService.lagreAndelerForBehandling(saksbehandling)
    }

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørDagligReiseRequest,
    ) {
        feilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(vedtak.opphørsdato == null) {
            "Opphørsdato er ikke satt"
        }
        val opphørsdato = vedtak.opphørsdato
        val forrigeVedtak = dagligReiseVedtakService.hentInnvilgelseEllerOpphørVedtak(saksbehandling.forrigeIverksatteBehandlingId)

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortetVedtaksperioder = dagligReiseVedtakService.avkortVedtaksperiodeVedOpphør(forrigeVedtak, opphørsdato)

        val beregningsplan = beregningsplanUtleder.utledForOpphør(opphørsdato)
        val (beregningsresultat, _) =
            beregningService.beregn(
                vedtaksperioder = avkortetVedtaksperioder,
                behandling = saksbehandling,
                beregningsplan = beregningsplan,
                typeVedtak = TypeVedtak.OPPHØR,
            )
        opphørValideringService.validerIngenUtbetalingEtterOpphørsdatoDagligReise(
            beregningsresultat,
            opphørsdato,
        )

        dagligReiseVedtakService.lagreOpphørsvedtak(
            saksbehandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            rammevedtakPrivatBil = null, // TODO: Håndter rammevedtak i opphør
            avkortetVedtaksperioder = avkortetVedtaksperioder,
            vedtak = vedtak,
            beregningsplan = beregningsplan,
        )

        opprettAndelerDagligReiseService.lagreAndelerForBehandling(saksbehandling)
    }
}
