package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.stereotype.Service
import java.time.DayOfWeek

@Service
class TilsynBarnBeregnYtelseSteg(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    private val opphørValideringService: OpphørValideringService,
    vedtakRepository: VedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakTilsynBarnDto>(
    stønadstype = Stønadstype.BARNETILSYN,
    vedtakRepository = vedtakRepository,
    tilkjentytelseService = tilkjentytelseService,
    simuleringService = simuleringService,
) {

    override fun lagreVedtak(saksbehandling: Saksbehandling, vedtak: VedtakTilsynBarnDto) {
        when (vedtak) {
            is InnvilgelseTilsynBarnDto -> beregnOgLagreInnvilgelse(saksbehandling)
            is AvslagTilsynBarnDto -> lagreAvslag(saksbehandling, vedtak)
            is OpphørTilsynBarnDto -> beregnOgLagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun beregnOgLagreInnvilgelse(saksbehandling: Saksbehandling) {
        val beregningsresultat = tilsynBarnBeregningService.beregn(saksbehandling, TypeVedtak.INNVILGELSE)
        vedtakRepository.insert(lagInnvilgetVedtak(saksbehandling, beregningsresultat))
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(saksbehandling: Saksbehandling, vedtak: OpphørTilsynBarnDto) {
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
                data = OpphørTilsynBarn(
                    beregningsresultat = BeregningsresultatTilsynBarn(beregningsresultat.perioder),
                    årsaker = vedtak.årsakerOpphør,
                    begrunnelse = vedtak.begrunnelse,
                ),

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
                data = AvslagTilsynBarn(
                    årsaker = vedtak.årsakerAvslag,
                    begrunnelse = vedtak.begrunnelse,
                ),
            ),
        )
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatTilsynBarn,
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
        beregningsresultat: BeregningsresultatTilsynBarn,
    ): Vedtak {
        return GeneriskVedtak(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGELSE,
            data = InnvilgelseTilsynBarn(
                beregningsresultat = BeregningsresultatTilsynBarn(beregningsresultat.perioder),
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
}
