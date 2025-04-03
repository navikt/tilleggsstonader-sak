package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterAndelTilkjentYtelseMapper.finnAndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregningService
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import org.springframework.stereotype.Service

@Service
class BoutgifterBeregnYtelseSteg(
    private val beregningService: BoutgifterBeregningService,
//    private val opphørValideringService: OpphørValideringService,
//    private val vedtaksperiodeService: VedtaksperiodeService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakBoutgifterRequest>(
        stønadstype = Stønadstype.BOUTGIFTER,
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakBoutgifterRequest,
    ) {
        when (vedtak) {
            is InnvilgelseBoutgifterRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
//            is AvslagTilsynBarnDto -> lagreAvslag(saksbehandling, vedtak)
//            is OpphørTilsynBarnRequest -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseBoutgifterRequest,
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
        tilkjentYtelseService.lagreTilkjentYtelse(
            saksbehandling = saksbehandling,
            andeler =
                finnAndelTilkjentYtelse(
                    saksbehandling,
                    beregningsresultat,
                ),
        )
    }

//    private fun beregnOgLagreOpphør(
//        saksbehandling: Saksbehandling,
//        vedtak: OpphørTilsynBarnRequest,
//    ) {
//        brukerfeilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
//            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
//        }
//
//        opphørValideringService.validerVilkårperioder(saksbehandling)
//
//        val vedtaksperioder = vedtaksperiodeService.finnNyeVedtaksperioderForOpphør(saksbehandling)
//
//        val beregningsresultat =
//            beregningService.beregn(
//                vedtaksperioder = vedtaksperioder,
//                behandling = saksbehandling,
//                typeVedtak = TypeVedtak.OPPHØR,
//            )
//        opphørValideringService.validerIngenUtbetalingEtterRevurderFraDato(
//            beregningsresultat,
//            saksbehandling.revurderFra,
//        )
//        vedtakRepository.insert(
//            GeneriskVedtak(
//                behandlingId = saksbehandling.id,
//                type = TypeVedtak.OPPHØR,
//                data =
//                    OpphørTilsynBarn(
//                        beregningsresultat = BeregningsresultatTilsynBarn(beregningsresultat.perioder),
//                        årsaker = vedtak.årsakerOpphør,
//                        begrunnelse = vedtak.begrunnelse,
//                        vedtaksperioder = vedtaksperioder,
//                    ),
//                gitVersjon = Applikasjonsversjon.versjon,
//            ),
//        )
//
//        lagreAndeler(saksbehandling, beregningsresultat)
//    }

//    private fun lagreAvslag(
//        saksbehandling: Saksbehandling,
//        vedtak: AvslagTilsynBarnDto,
//    ) {
//        vedtakRepository.insert(
//            GeneriskVedtak(
//                behandlingId = saksbehandling.id,
//                type = TypeVedtak.AVSLAG,
//                data =
//                    AvslagTilsynBarn(
//                        årsaker = vedtak.årsakerAvslag,
//                        begrunnelse = vedtak.begrunnelse,
//                    ),
//                gitVersjon = Applikasjonsversjon.versjon,
//            ),
//        )
//    }

//    private fun lagreAndeler(
//        saksbehandling: Saksbehandling,
//        beregningsresultat: BeregningsresultatTilsynBarn,
//    ) {
//        val andelerTilkjentYtelse =
//            beregningsresultat.perioder.flatMap {
//                it.beløpsperioder.map { beløpsperiode ->
//                    val satstype = Satstype.DAG
//                    val ukedag = beløpsperiode.dato.dayOfWeek
//                    feilHvis(ukedag == DayOfWeek.SATURDAY || ukedag == DayOfWeek.SUNDAY) {
//                        "Skal ikke opprette perioder som begynner på en helgdag for satstype=$satstype"
//                    }
//                    val førsteDagIMåneden =
//                        beløpsperiode.dato
//                            .toYearMonth()
//                            .atDay(1)
//                            .datoEllerNesteMandagHvisLørdagEllerSøndag()
//                    AndelTilkjentYtelse(
//                        beløp = beløpsperiode.beløp,
//                        fom = beløpsperiode.dato,
//                        tom = beløpsperiode.dato,
//                        satstype = satstype,
//                        type = beløpsperiode.målgruppe.tilTypeAndel(Stønadstype.BARNETILSYN),
//                        kildeBehandlingId = saksbehandling.id,
//                        utbetalingsdato = førsteDagIMåneden,
//                    )
//                }
//            }
//
//        tilkjentytelseService.opprettTilkjentYtelse(saksbehandling, andelerTilkjentYtelse)
//    }

    private fun lagInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatBoutgifter,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
    ): Vedtak =
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
        )
}
