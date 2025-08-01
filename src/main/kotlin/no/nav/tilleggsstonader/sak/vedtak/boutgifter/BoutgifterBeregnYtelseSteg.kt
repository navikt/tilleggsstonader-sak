package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
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
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregningService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BoutgifterBeregnYtelseSteg(
    private val beregningService: BoutgifterBeregningService,
    private val opphørValideringService: OpphørValideringService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    unleashService: UnleashService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakBoutgifterRequest>(
        stønadstype = Stønadstype.BOUTGIFTER,
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
        unleashService = unleashService,
    ) {
    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakBoutgifterRequest,
    ) {
        when (vedtak) {
            is InnvilgelseBoutgifterRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
            is AvslagBoutgifterDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørBoutgifterRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseBoutgifterRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder.tilDomene().sorted()
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                saksbehandling.id,
                vedtaksperioder,
            )
        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
                tidligsteEndring = tidligsteEndring,
            )
        lagreInnvilgetVedtak(
            behandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = vedtak.begrunnelse,
            tidligsteEndring = tidligsteEndring,
        )
        lagreTilkjentYtelse(saksbehandling.id, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørBoutgifterRequest,
    ) {
        feilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }

        val opphørsdato =
            revurderFraEllerOpphørsdato(
                revurderFra = saksbehandling.revurderFra,
                opphørsdato = vedtak.opphørsdato,
            )

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        val forrigeVedtak = hentVedtak(saksbehandling.forrigeIverksatteBehandlingId)

        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortedeVedtaksperioder = avkortVedtaksperiodeVedOpphør(forrigeVedtak, opphørsdato)

        val beregningsresultat =
            beregningService.beregn(
                saksbehandling,
                avkortedeVedtaksperioder,
                TypeVedtak.OPPHØR,
                opphørsdato,
            )

        lagreOpphørsvedtak(saksbehandling, avkortedeVedtaksperioder, beregningsresultat, vedtak)
        lagreTilkjentYtelse(saksbehandling.id, beregningsresultat)
    }

    private fun avkortVedtaksperiodeVedOpphør(
        forrigeVedtak: GeneriskVedtak<out InnvilgelseEllerOpphørBoutgifter>,
        revurderFra: LocalDate,
    ): List<Vedtaksperiode> = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(revurderFra.minusDays(1))

    private fun hentVedtak(behandlingId: BehandlingId): GeneriskVedtak<InnvilgelseEllerOpphørBoutgifter> =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagBoutgifterDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagBoutgifter(
                        årsaker = vedtak.årsakerAvslag,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            ),
        )
    }

    private fun lagreInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatBoutgifter,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        tidligsteEndring: LocalDate?,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseBoutgifter(
                        vedtaksperioder = vedtaksperioder,
                        begrunnelse = begrunnelse,
                        beregningsresultat = BeregningsresultatBoutgifter(beregningsresultat.perioder),
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = if (unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK)) tidligsteEndring else null,
            ),
        )
    }

    private fun lagreOpphørsvedtak(
        saksbehandling: Saksbehandling,
        avkortedeVedtaksperioder: List<Vedtaksperiode>,
        beregningsresultat: BeregningsresultatBoutgifter,
        vedtak: OpphørBoutgifterRequest,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørBoutgifter(
                        vedtaksperioder = avkortedeVedtaksperioder,
                        beregningsresultat = beregningsresultat,
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = vedtak.opphørsdato,
            ),
        )
    }

    private fun lagreTilkjentYtelse(
        behandlingId: BehandlingId,
        beregningsresultat: BeregningsresultatBoutgifter,
    ) {
        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = behandlingId,
            andeler = beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId),
        )
    }
}
