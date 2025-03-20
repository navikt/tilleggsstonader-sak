package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
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
import org.springframework.stereotype.Service
import java.time.DayOfWeek

@Service
class TilsynBarnBeregnYtelseSteg(
    private val beregningService: TilsynBarnBeregningService,
    private val opphørValideringService: OpphørValideringService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    vedtakRepository: VedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakTilsynBarnRequest>(
        stønadstype = Stønadstype.BARNETILSYN,
        vedtakRepository = vedtakRepository,
        tilkjentytelseService = tilkjentytelseService,
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
        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene(),
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )
        vedtakRepository.insert(
            lagInnvilgetVedtak(
                behandling = saksbehandling,
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtak.vedtaksperioder.tilDomene().sorted(),
                begrunnelse = vedtak.begrunnelse,
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

        val vedtaksperioder = vedtaksperiodeService.finnNyeVedtaksperioderForOpphør(saksbehandling)

        val beregningsresultat =
            beregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                behandling = saksbehandling,
                typeVedtak = TypeVedtak.OPPHØR,
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
            ),
        )
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatTilsynBarn,
    ) {
        val andelerTilkjentYtelse =
            beregningsresultat.perioder.flatMap {
                it.beløpsperioder.map { beløpsperiode ->
                    val satstype = Satstype.DAG
                    val ukedag = beløpsperiode.dato.dayOfWeek
                    feilHvis(ukedag == DayOfWeek.SATURDAY || ukedag == DayOfWeek.SUNDAY) {
                        "Skal ikke opprette perioder som begynner på en helgdag for satstype=$satstype"
                    }
                    val førsteDagIMåneden =
                        beløpsperiode.dato
                            .toYearMonth()
                            .atDay(1)
                            .datoEllerNesteMandagHvisLørdagEllerSøndag()
                    AndelTilkjentYtelse(
                        beløp = beløpsperiode.beløp,
                        fom = beløpsperiode.dato,
                        tom = beløpsperiode.dato,
                        satstype = satstype,
                        type = beløpsperiode.målgruppe.tilTypeAndel(Stønadstype.BARNETILSYN),
                        kildeBehandlingId = saksbehandling.id,
                        utbetalingsdato = førsteDagIMåneden,
                    )
                }
            }

        tilkjentytelseService.opprettTilkjentYtelse(saksbehandling, andelerTilkjentYtelse)
    }

    private fun lagInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatTilsynBarn,
        vedtaksperioder: List<Vedtaksperiode>?,
        begrunnelse: String?,
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
        )
}
