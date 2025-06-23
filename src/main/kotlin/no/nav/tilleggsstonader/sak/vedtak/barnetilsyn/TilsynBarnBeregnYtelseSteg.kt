package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilsynBarnBeregnYtelseSteg(
    private val beregningService: TilsynBarnBeregningService,
    private val opphørValideringService: OpphørValideringService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    private val unleashService: UnleashService,
    vedtakRepository: VedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakTilsynBarnRequest>(
        stønadstype = Stønadstype.BARNETILSYN,
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentytelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakTilsynBarnRequest,
    ) {
        when (vedtak) {
            is InnvilgelseTilsynBarnRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
            is AvslagTilsynBarnDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørTilsynBarnRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseTilsynBarnRequest,
    ) {
        val beregnFraDato = utledTidligsteEndringService.utledTidligsteEndring(saksbehandling.id)

        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene(),
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
                beregnFraDato = beregnFraDato,
            )
        vedtakRepository.insert(
            lagInnvilgetVedtak(
                behandling = saksbehandling,
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene().sorted(),
                begrunnelse = vedtak.begrunnelse,
                beregnetFra = beregnFraDato,
            ),
        )
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørTilsynBarnRequest,
    ) {
        brukerfeilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }

        opphørValideringService.validerVilkårperioder(saksbehandling)

        // TODO - send med opphørsdato (ny)
        val vedtaksperioder = vedtaksperiodeService.finnNyeVedtaksperioderForOpphør(saksbehandling)

        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.OPPHØR,
                // TODO: Sette beregnFraDato til opphørsdato (ny)
                beregnFraDato = saksbehandling.revurderFra,
            )
        opphørValideringService.validerIngenUtbetalingEtterRevurderFraDato(
            beregningsresultat,
            saksbehandling.revurderFra,
        )
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørTilsynBarn(
                        beregningsresultat = BeregningsresultatTilsynBarn(beregningsresultat.perioder),
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                        vedtaksperioder = vedtaksperioder,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                beregnetFra = null, // TODO: Sette beregnFraDato til opphørsdato (ny)
            ),
        )

        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagTilsynBarnDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagTilsynBarn(
                        årsaker = vedtak.årsakerAvslag,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                beregnetFra = null,
            ),
        )
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatTilsynBarn,
    ) {
        val andelerTilkjentYtelse = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling)
        tilkjentYtelseService.lagreTilkjentYtelse(saksbehandling.id, andelerTilkjentYtelse)
    }

    private fun lagInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatTilsynBarn,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        beregnetFra: LocalDate?,
    ): Vedtak =
        GeneriskVedtak(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGELSE,
            data =
                InnvilgelseTilsynBarn(
                    vedtaksperioder = vedtaksperioder,
                    begrunnelse = begrunnelse,
                    beregningsresultat = BeregningsresultatTilsynBarn(beregningsresultat.perioder),
                ),
            gitVersjon = Applikasjonsversjon.versjon,
            beregnetFra = if (unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK)) beregnetFra else null,
        )
}
