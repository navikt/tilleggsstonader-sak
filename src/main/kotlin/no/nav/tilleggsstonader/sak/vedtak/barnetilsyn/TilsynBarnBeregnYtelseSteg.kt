package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.OpphørValidatorService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.stereotype.Service
import java.time.DayOfWeek

@Service
class TilsynBarnBeregnYtelseSteg(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    private val unleashService: UnleashService,
    private val opphørValidatorService: OpphørValidatorService,
    vedtakRepository: TilsynBarnVedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakTilsynBarnDto, VedtakTilsynBarn>(
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

    /**
     * Brukes både for innvilgelse og opphør
     * Ved opphør kan det være at man kun opphør 1 barn, men det fortsatt skal utbetales for det andre barnet
     */
    private fun beregnOgLagreInnvilgelse(saksbehandling: Saksbehandling) {
        brukerfeilHvis(
            saksbehandling.forrigeBehandlingId != null &&
                !unleashService.isEnabled(Toggle.REVURDERING_INNVILGE_TIDLIGERE_INNVILGET),
        ) {
            "Funksjonalitet mangler for å kunne innvilge revurdering når tidligere behandling er innvilget. Sett saken på vent."
        }

        val beregningsresultat = tilsynBarnBeregningService.beregn(saksbehandling)
        vedtakRepository.insert(lagInnvilgetVedtak(saksbehandling, beregningsresultat))
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun beregnOgLagreOpphør(saksbehandling: Saksbehandling, vedtak: OpphørTilsynBarnDto) {
        opphørValidatorService.validerOpphør(saksbehandling)

        val beregningsresultat = tilsynBarnBeregningService.beregn(saksbehandling)
        vedtakRepository.insert(
            VedtakTilsynBarn(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                beregningsresultat = BeregningsresultatTilsynBarn(beregningsresultat.perioder),
                årsakerOpphør = ÅrsakOpphør.Wrapper(årsaker = vedtak.årsakerOpphør),
                opphørBegrunnelse = vedtak.begrunnelse,
            ),
        )
        lagreAndeler(saksbehandling, beregningsresultat)
    }

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagTilsynBarnDto,
    ) {
        vedtakRepository.insert(
            VedtakTilsynBarn(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                avslagBegrunnelse = vedtak.begrunnelse,
                årsakerAvslag = ÅrsakAvslag.Wrapper(årsaker = vedtak.årsakerAvslag),
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
    ): VedtakTilsynBarn {
        return VedtakTilsynBarn(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGELSE,
            vedtak = VedtaksdataTilsynBarn(
                utgifter = emptyMap(),
            ),
            beregningsresultat = BeregningsresultatTilsynBarn(beregningsresultat.perioder),
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
