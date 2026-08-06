package no.nav.tilleggsstonader.sak.vedtak.passAvBarn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelsePassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.beregning.PassAvBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.AvslagPassAvBarnDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.InnvilgelsePassAvBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.OpphørPassAvBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.VedtakPassAvBarnRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PassAvBarnBeregnYtelseSteg(
    private val beregningService: PassAvBarnBeregningService,
    private val opphørValideringService: OpphørValideringService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    vedtakRepository: VedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakPassAvBarnRequest>(
        stønadstype = listOf(Stønadstype.BARNETILSYN),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentytelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: VedtakPassAvBarnRequest,
        satsjusteringFra: LocalDate,
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakPassAvBarnRequest,
    ) {
        when (vedtak) {
            is InnvilgelsePassAvBarnRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
            is AvslagPassAvBarnDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørPassAvBarnRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelsePassAvBarnRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder.tilDomene()
        val beregningsplan = beregningsplanUtleder.utledForInnvilgelse(saksbehandling, vedtaksperioder)
        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                plan = beregningsplan,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )
        vedtakRepository.insert(
            lagInnvilgetVedtak(
                behandling = saksbehandling,
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtaksperioder.sorted(),
                begrunnelse = vedtak.begrunnelse,
                beregningsplan = beregningsplan,
            ),
        )
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørPassAvBarnRequest,
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

        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(opphørsdato)

        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                plan = beregningsplan,
                typeVedtak = TypeVedtak.OPPHØR,
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
                    OpphørPassAvBarn(
                        beregningsresultat = BeregningsresultatPassAvBarn(beregningsresultat.perioder),
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                        vedtaksperioder = vedtaksperioder,
                        beregningsplan = beregningsplan,
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
        vedtak: AvslagPassAvBarnDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data =
                    AvslagPassAvBarn(
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
        beregningsresultat: BeregningsresultatPassAvBarn,
    ) {
        val andelerTilkjentYtelse = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling)
        tilkjentYtelseService.lagreTilkjentYtelse(saksbehandling.id, andelerTilkjentYtelse)
    }

    private fun lagInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatPassAvBarn,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        beregningsplan: Beregningsplan,
    ): Vedtak =
        GeneriskVedtak(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGELSE,
            data =
                InnvilgelsePassAvBarn(
                    vedtaksperioder = vedtaksperioder,
                    begrunnelse = begrunnelse,
                    beregningsresultat = BeregningsresultatPassAvBarn(beregningsresultat.perioder),
                    beregningsplan = beregningsplan,
                ),
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = beregningsplan.legacyTidligsteEndring(),
        )
}
