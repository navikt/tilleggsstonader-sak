package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
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
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilsynBarnBeregnYtelseSteg(
    private val beregningService: TilsynBarnBeregningService,
    private val opphørValideringService: OpphørValideringService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    vedtakRepository: VedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakTilsynBarnRequest>(
        stønadstype = listOf(Stønadstype.BARNETILSYN),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentytelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: VedtakTilsynBarnRequest,
        satsjusteringFra: LocalDate,
    ) {
        TODO("Not yet implemented")
    }

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
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                saksbehandling.id,
                vedtak.vedtaksperioder.tilDomene(),
            )

        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene(),
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
                tidligsteEndring = tidligsteEndring,
            )
        vedtakRepository.insert(
            lagInnvilgetVedtak(
                behandling = saksbehandling,
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene().sorted(),
                begrunnelse = vedtak.begrunnelse,
                tidligsteEndring = tidligsteEndring,
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
        feilHvis(vedtak.opphørsdato == null) {
            "Opphørsdato er ikke satt"
        }

        val opphørsdato = vedtak.opphørsdato

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)

        val vedtaksperioder = finnNyeVedtaksperioderForOpphør(saksbehandling, opphørsdato)

        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.OPPHØR,
                tidligsteEndring = opphørsdato,
            )
        opphørValideringService.validerIngenUtbetalingEtterOpphørsdato(
            beregningsresultat,
            opphørsdato,
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
                tidligsteEndring = null,
                opphørsdato = vedtak.opphørsdato,
            ),
        )

        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun finnNyeVedtaksperioderForOpphør(
        behandling: Saksbehandling,
        opphørsdato: LocalDate,
    ): List<Vedtaksperiode> {
        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi behandlingen er en førstegangsbehandling"
        }

        val forrigeVedtaksperioder =
            vedtakRepository.findByIdOrNull(behandling.forrigeIverksatteBehandlingId)?.vedtaksperioderHvisFinnes()

        feilHvis(forrigeVedtaksperioder == null) {
            "Kan ikke opphøre fordi data fra forrige vedtak mangler"
        }

        // .minusDays(1) fordi dagen før opphørsdato blir siste dag i vedtaksperioden
        return forrigeVedtaksperioder.avkortFraOgMed(opphørsdato.minusDays(1))
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
                tidligsteEndring = null,
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
        tidligsteEndring: LocalDate?,
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
            tidligsteEndring = tidligsteEndring,
        )
}
