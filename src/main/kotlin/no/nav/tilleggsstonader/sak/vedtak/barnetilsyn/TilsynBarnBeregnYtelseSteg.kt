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
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class TilsynBarnBeregnYtelseSteg(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    private val vilkårService: VilkårService,
    private val unleashService: UnleashService,
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
            is InnvilgelseTilsynBarnDto -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
            is AvslagTilsynBarnDto -> lagreAvslag(saksbehandling, vedtak)
        }
        /*
        Funksjonalitet som mangler:
         * Revurdering
         * Opphør

         Simulering burde kanskje kun gjøres når man går inn på fanen for simulering,
         og ikke i dette steget for å unngå feil fra simulering
         */
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseTilsynBarnDto,
    ) {
        brukerfeilHvis(
            saksbehandling.forrigeBehandlingId != null &&
                !unleashService.isEnabled(Toggle.REVURDERING_INNVILGE_TIDLIGERE_INNVILGET),
        ) {
            "Funksjonalitet mangler for å kunne innvilge revurdering når tidligere behandling er innvilget. Sett saken på vent."
        }

        val beregningsresultat =
            tilsynBarnBeregningService.beregn(behandlingId = saksbehandling.id, vedtak.utgifter)
        validerKunBarnMedOppfylteVilkår(saksbehandling, vedtak)
        vedtakRepository.insert(lagInnvilgetVedtak(saksbehandling, vedtak, beregningsresultat))
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

    private fun validerKunBarnMedOppfylteVilkår(saksbehandling: Saksbehandling, vedtak: InnvilgelseTilsynBarnDto) {
        val barnMedOppfylteVilkår =
            vilkårService.hentOppfyltePassBarnVilkår(behandlingId = saksbehandling.id).map { it.barnId }
        val barnUtenOppfylteVilkår = vedtak.utgifter.keys.filter { !barnMedOppfylteVilkår.contains(it) }

        feilHvis(barnUtenOppfylteVilkår.isNotEmpty()) {
            "Det finnes utgifter på barn som ikke har oppfylt vilkårsvurdering, id=$barnUtenOppfylteVilkår"
        }
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatTilsynBarnDto,
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
        }.toSet()

        tilkjentytelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
            ),
        )
    }

    private fun lagInnvilgetVedtak(
        behandling: Saksbehandling,
        vedtak: InnvilgelseTilsynBarnDto,
        beregningsresultat: BeregningsresultatTilsynBarnDto,
    ): VedtakTilsynBarn {
        return VedtakTilsynBarn(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGELSE,
            vedtak = VedtaksdataTilsynBarn(
                utgifter = vedtak.utgifter,
            ),
            beregningsresultat = VedtaksdataBeregningsresultat(beregningsresultat.perioder),
        )
    }

    /**
     * Vi lagrer ned startdato for å eventuellt kunne opphøre bak i tiden for perioder som Arena eier.
     */
    private fun beregnStartdato(
        saksbehandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): LocalDate {
        feilHvis(saksbehandling.forrigeBehandlingId != null) {
            "Når vi begynner å revurdere og opphøre må vi oppdatere denne metoden for å finne startdato"
        }
        return minOf(andelerTilkjentYtelse.minOf { it.fom })
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
