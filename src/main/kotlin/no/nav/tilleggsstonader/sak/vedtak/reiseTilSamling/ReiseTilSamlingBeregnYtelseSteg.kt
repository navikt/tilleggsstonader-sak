package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.OpprettAndelerReiseTilSamlingService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.AvslagReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.InnvilgelseReiseTilSamlingRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.OpphørReiseTilSamlingRequest
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto.VedtakReiseTilSamlingRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReiseTilSamlingBeregnYtelseSteg(
    private val beregningService: ReiseTilSamlingBeregningService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    private val opprettAndelerReiseTilSamlingService: OpprettAndelerReiseTilSamlingService,
    private val opphørValideringService: OpphørValideringService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakReiseTilSamlingRequest>(
        stønadstype = listOf(Stønadstype.REISE_TIL_SAMLING_TSO, Stønadstype.REISE_TIL_SAMLING_TSR),
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
            is AvslagReiseTilSamlingDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørReiseTilSamlingRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagReiseTilSamlingDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagReiseTilSamling(
                        årsaker = vedtak.årsakerAvslag,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            ),
        )
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

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørReiseTilSamlingRequest,
    ) {
        feilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(vedtak.opphørsdato == null) {
            "Opphørsdato er ikke satt"
        }
        val opphørsdato = vedtak.opphørsdato

        val forrigeVedtak =
            vedtakRepository
                .findByIdOrThrow(saksbehandling.forrigeIverksatteBehandlingId)
                .withTypeOrThrow<InnvilgelseEllerOpphørReiseTilSamling>()

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortetVedtaksperioder = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(opphørsdato.minusDays(1))

        val beregningsplan =
            BeregningsplanUtleder.utledForOpphørEllerSatsjustering(
                opphørsdato = opphørsdato,
            )

        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = avkortetVedtaksperioder,
                behandling = saksbehandling,
                beregningsplan = beregningsplan,
                typeVedtak = TypeVedtak.OPPHØR,
            )

        opphørValideringService.validerIngenUtbetalingEtterOpphørsdatoReiseTilSamling(
            beregningsresultat,
            opphørsdato,
        )

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørReiseTilSamling(
                        beregningsresultat = beregningsresultat,
                        vedtaksperioder = avkortetVedtaksperioder,
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                        beregningsplan = beregningsplan,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = opphørsdato,
            ),
        )

        opprettAndelerReiseTilSamlingService.lagreAndelerForBehandling(saksbehandling)
    }
}
