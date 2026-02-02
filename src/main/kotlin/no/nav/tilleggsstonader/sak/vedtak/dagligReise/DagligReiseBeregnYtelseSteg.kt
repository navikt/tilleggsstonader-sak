package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.DagligReiseBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.tilDomene
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseBeregnYtelseSteg(
    private val beregningService: DagligReiseBeregningService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    private val opphørValideringService: OpphørValideringService,
    private val dagligReiseVedtakService: DagligReiseVedtakService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakDagligReiseRequest>(
        stønadstype = listOf(Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
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
        val vedtaksperioder = vedtak.vedtaksperioder.tilDomene()

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                saksbehandling.id,
                vedtaksperioder,
            )
        val (beregningsresultat, _) =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
                tidligsteEndring = tidligsteEndring,
            )
        dagligReiseVedtakService.lagreInnvilgetVedtak(
            behandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            rammevedtakPrivatBil = null,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = vedtak.begrunnelse,
            tidligsteEndring = tidligsteEndring,
        )

        feilHvis(beregningsresultat.offentligTransport == null) {
            "Foreløpig støttes kun beregning av offentlig transport."
        }

        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = beregningsresultat.offentligTransport.mapTilAndelTilkjentYtelse(saksbehandling),
        )
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
        val forrigeVedtak = dagligReiseVedtakService.hentVedtak(saksbehandling.forrigeIverksatteBehandlingId)

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortetVedtaksperioder = dagligReiseVedtakService.avkortVedtaksperiodeVedOpphør(forrigeVedtak, opphørsdato)

        val (beregningsresultat, _) =
            beregningService.beregn(
                vedtaksperioder = avkortetVedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.OPPHØR,
                tidligsteEndring = opphørsdato,
            )
        opphørValideringService.validerIngenUtbetalingEtterOpphørsdatoDagligReise(
            beregningsresultat,
            opphørsdato,
        )

        dagligReiseVedtakService.lagreOpphørsvedtak(
            saksbehandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            rammevedtakPrivatBil = null,
            avkortetVedtaksperioder = avkortetVedtaksperioder,
            vedtak = vedtak,
        )

        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler =
                beregningsresultat.offentligTransport?.mapTilAndelTilkjentYtelse(saksbehandling) ?: feil(
                    "Mangler beregningsresultat for offentlig transport",
                ),
        )
    }
}
