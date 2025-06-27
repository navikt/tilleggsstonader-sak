package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.læremidler.VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.avkortVedtaksperiodeVedOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDomene
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LæremidlerBeregnYtelseSteg(
    private val beregningService: LæremidlerBeregningService,
    private val opphørValideringService: OpphørValideringService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
    unleashService: UnleashService,
) : BeregnYtelseSteg<VedtakLæremidlerRequest>(
        stønadstype = Stønadstype.LÆREMIDLER,
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
        unleashService = unleashService,
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

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
    ) {
        val forrigeVedtaksperioder =
            saksbehandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it).data.vedtaksperioder }
        val vedtaksperioderMedStatus =
            settStatusPåVedtaksperioder(
                vedtaksperioder = vedtaksperioder,
                vedtaksperioderForrigeBehandling = forrigeVedtaksperioder,
            )

        lagreVedtak(saksbehandling, vedtaksperioderMedStatus, begrunnelse)
    }

    private fun hentVedtak(behandlingId: BehandlingId): GeneriskVedtak<InnvilgelseEllerOpphørLæremidler> =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>()

    private fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
    ) {
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndring(
                saksbehandling.id,
                vedtaksperioder.map {
                    it.tilFellesDomeneVedtaksperiode()
                },
            )

        val beregningsresultat =
            beregningService.beregn(
                behandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
                tidligsteEndring = tidligsteEndring,
            )

        vedtakRepository.insert(
            lagInnvilgetVedtak(
                behandlingId = saksbehandling.id,
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = beregningsresultat,
                begrunnelse = begrunnelse,
                tidligsteEndring = tidligsteEndring,
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

        val opphørsdato =
            revurderFraEllerOpphørsdato(
                revurderFra = saksbehandling.revurderFra,
                opphørsdato = vedtak.opphørsdato,
            )
        val forrigeVedtak = hentVedtak(saksbehandling.forrigeIverksatteBehandlingId)

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphørLæremidler(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortetVedtaksperioder = avkortVedtaksperiodeVedOpphør(forrigeVedtak, opphørsdato)

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
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = vedtak.opphørsdato,
            ),
        )

        lagreAndeler(saksbehandling, beregningsresultat)
    }

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
                opphørsdato = null,
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

    private fun lagInnvilgetVedtak(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
        beregningsresultat: BeregningsresultatLæremidler,
        begrunnelse: String?,
        tidligsteEndring: LocalDate?,
    ): Vedtak =
        GeneriskVedtak(
            behandlingId = behandlingId,
            type = TypeVedtak.INNVILGELSE,
            data =
                InnvilgelseLæremidler(
                    vedtaksperioder = vedtaksperioder,
                    beregningsresultat = BeregningsresultatLæremidler(beregningsresultat.perioder),
                    begrunnelse = begrunnelse,
                ),
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = if (unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK)) tidligsteEndring else null,
            opphørsdato = null,
        )
}
