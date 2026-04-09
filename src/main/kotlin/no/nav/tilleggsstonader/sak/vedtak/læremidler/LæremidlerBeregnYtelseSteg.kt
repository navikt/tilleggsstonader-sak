package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LæremidlerBeregnYtelseSteg(
    private val beregningService: LæremidlerBeregningService,
    private val opphørValideringService: OpphørValideringService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakLæremidlerRequest>(
        stønadstype = listOf(Stønadstype.LÆREMIDLER),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakLæremidlerRequest,
    ) {
        when (vedtak) {
            is InnvilgelseLæremidlerRequest ->
                beregnOgLagreInnvilgelse(
                    saksbehandling = saksbehandling,
                    vedtaksperioder = vedtak.vedtaksperioder.tilDomene(),
                    begrunnelse = vedtak.begrunnelse,
                )

            is AvslagLæremidlerDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørLæremidlerRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: VedtakLæremidlerRequest,
        satsjusteringFra: LocalDate,
    ) {
        logger.info("Lagrer vedtak for satsjustering for behandling=${saksbehandling.id}, satsjusteringFra=$satsjusteringFra")

        val innvilgelse = vedtak as InnvilgelseLæremidlerRequest
        val plan = Beregningsplan(Beregningsomfang.FRA_DATO, satsjusteringFra)
        lagreInnvilgetVedtak(
            saksbehandling = saksbehandling,
            vedtaksperioder = innvilgelse.vedtaksperioder.tilDomene(),
            begrunnelse = null,
            beregningsplan = plan,
        )
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
    ) {
        lagreInnvilgetVedtak(
            saksbehandling = saksbehandling,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = begrunnelse,
            beregningsplan = beregningsplanUtleder.utledForInnvilgelse(saksbehandling, vedtaksperioder),
        )
    }

    private fun hentVedtak(behandlingId: BehandlingId): GeneriskVedtak<InnvilgelseEllerOpphørLæremidler> =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>()

    private fun lagreInnvilgetVedtak(
        saksbehandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        beregningsplan: Beregningsplan,
    ) {
        val beregningsresultat =
            beregningService.beregn(
                behandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
                plan = beregningsplan,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseLæremidler(
                        vedtaksperioder = vedtaksperioder,
                        beregningsresultat = BeregningsresultatLæremidler(beregningsresultat.perioder),
                        begrunnelse = begrunnelse,
                        beregningsplan = beregningsplan,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = beregningsplan.beregnFra(),
            ),
        )
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørLæremidlerRequest,
    ) {
        feilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(vedtak.opphørsdato == null) {
            "Opphørsdato er ikke satt"
        }

        val opphørsdato = vedtak.opphørsdato
        val forrigeVedtak = hentVedtak(saksbehandling.forrigeIverksatteBehandlingId)

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortetVedtaksperioder = avkortVedtaksperiodeVedOpphør(forrigeVedtak, opphørsdato)
        val beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, opphørsdato)
        val beregningsresultat = beregningService.beregnForOpphør(saksbehandling, avkortetVedtaksperioder, opphørsdato)

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørLæremidler(
                        vedtaksperioder = avkortetVedtaksperioder,
                        beregningsresultat = beregningsresultat,
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                        beregningsplan = beregningsplan,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = vedtak.opphørsdato,
            ),
        )

        lagreAndeler(saksbehandling, beregningsresultat)
    }

    fun avkortVedtaksperiodeVedOpphør(
        forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørLæremidler>,
        opphørsdato: LocalDate,
    ): List<Vedtaksperiode> = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(opphørsdato.minusDays(1))

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagLæremidlerDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagLæremidler(
                        årsaker = vedtak.årsakerAvslag,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            ),
        )
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatLæremidler,
    ) {
        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling.id)
        tilkjentYtelseService.lagreTilkjentYtelse(saksbehandling.id, andeler)
    }
}
