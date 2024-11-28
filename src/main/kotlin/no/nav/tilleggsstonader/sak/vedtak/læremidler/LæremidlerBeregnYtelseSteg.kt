package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import org.springframework.stereotype.Service

@Service
class LæremidlerBeregnYtelseSteg(
    private val beregningService: LæremidlerBeregningService,
    private val opphørValideringService: OpphørValideringService,
    vedtakRepository: VedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakLæremidlerRequest>(
    stønadstype = Stønadstype.LÆREMIDLER,
    vedtakRepository = vedtakRepository,
    tilkjentytelseService = tilkjentytelseService,
    simuleringService = simuleringService,
) {

    override fun lagreVedtak(saksbehandling: Saksbehandling, vedtak: VedtakLæremidlerRequest) {
        when (vedtak) {
            // is InnvilgelseLæremidlerRequest -> beregnOgLagreInnvilgelse(saksbehandling)
            is AvslagLæremidlerDto -> lagreAvslag(saksbehandling, vedtak)
            // is OpphørLæremidlerDto -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    /*
    private fun beregnOgLagreInnvilgelse(saksbehandling: Saksbehandling) {
        val beregningsresultat = tilsynBarnBeregningService.beregn(saksbehandling, TypeVedtak.INNVILGELSE)
        vedtakRepository.insert(lagInnvilgetVedtak(saksbehandling, beregningsresultat))
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(saksbehandling: Saksbehandling, vedtak: OpphørLæremidlerDto) {
        brukerfeilHvis(saksbehandling.forrigeBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }

        opphørValideringService.validerPerioder(saksbehandling)

        val beregningsresultat = tilsynBarnBeregningService.beregn(saksbehandling, TypeVedtak.OPPHØR)
        opphørValideringService.validerIngenUtbetalingEtterRevurderFraDato(beregningsresultat, saksbehandling.revurderFra)
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data = OpphørLæremidler(
                    beregningsresultat = BeregningsresultatLæremidler(beregningsresultat.perioder),
                    årsaker = vedtak.årsakerOpphør,
                    begrunnelse = vedtak.begrunnelse,
                ),

                ),
        )

        lagreAndeler(saksbehandling, beregningsresultat)
    }
     */

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagLæremidlerDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data = AvslagLæremidler(
                    årsaker = vedtak.årsakerAvslag,
                    begrunnelse = vedtak.begrunnelse,
                ),
            ),
        )
    }

    /*
    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatLæremidler,
    ) {
        val andelerTilkjentYtelse = beregningsresultat.perioder.flatMap {
            it.beløpsperioder.map { beløpsperiode ->
                val satstype = Satstype.DAG
                val ukedag = beløpsperiode.dato.dayOfWeek
                feilHvis(ukedag == DayOfWeek.SATURDAY || ukedag == DayOfWeek.SUNDAY) {
                    "Skal ikke opprette perioder som begynner på en helgdag for satstype=$satstype"
                }
                AndelTilkjentYtelse(
                    beløp = beløpsperiode.beløp,
                    fom = beløpsperiode.dato,
                    tom = beløpsperiode.dato,
                    satstype = satstype,
                    type = beløpsperiode.målgruppe.tilTypeAndel(),
                    kildeBehandlingId = saksbehandling.id,
                )
            }
        }

        tilkjentytelseService.opprettTilkjentYtelse(saksbehandling, andelerTilkjentYtelse)
    }

    private fun lagInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: BeregningsresultatLæremidler,
    ): Vedtak {
        return GeneriskVedtak(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGELSE,
            data = InnvilgelseLæremidler(
                beregningsresultat = BeregningsresultatLæremidler(beregningsresultat.perioder),
            ),
        )
    }

    private fun MålgruppeType.tilTypeAndel(): TypeAndel {
        return when (this) {
            MålgruppeType.AAP, MålgruppeType.UFØRETRYGD, MålgruppeType.NEDSATT_ARBEIDSEVNE -> TypeAndel.TILSYN_BARN_AAP
            MålgruppeType.OVERGANGSSTØNAD -> TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER
            MålgruppeType.OMSTILLINGSSTØNAD -> TypeAndel.TILSYN_BARN_ETTERLATTE
            else -> error("Kan ikke opprette andel tilkjent ytelse for målgruppe $this")
        }
    }
     */
}
