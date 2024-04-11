package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilsynBarnBeregnYtelseSteg(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    private val vilkårService: VilkårService,
    vedtakRepository: TilsynBarnVedtakRepository,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<InnvilgelseTilsynBarnDto, VedtakTilsynBarn>(
    stønadstype = Stønadstype.BARNETILSYN,
    vedtakRepository = vedtakRepository,
    tilkjentytelseService = tilkjentytelseService,
    simuleringService = simuleringService,
) {

    override fun lagreVedtak(saksbehandling: Saksbehandling, vedtak: InnvilgelseTilsynBarnDto) {
        val beregningsresultat = tilsynBarnBeregningService.beregn(behandlingId = saksbehandling.id, vedtak.utgifter)
        validerKunBarnMedOppfylteVilkår(saksbehandling, vedtak)
        vedtakRepository.insert(lagVedtak(saksbehandling, vedtak, beregningsresultat))
        lagreAndeler(saksbehandling, beregningsresultat)
        /*
        Funksjonalitet som mangler:
         * Avslag
         * Revurdering
         * Opphør

         Simulering burde kanskje kun gjøres når man går inn på fanen for simulering,
         og ikke i dette steget for å unngå feil fra simulering
         */
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
                AndelTilkjentYtelse(
                    beløp = beløpsperiode.beløp,
                    fom = beløpsperiode.dato,
                    tom = beløpsperiode.dato,
                    satstype = Satstype.DAG,
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

    private fun lagVedtak(
        behandling: Saksbehandling,
        vedtak: InnvilgelseTilsynBarnDto,
        beregningsresultat: BeregningsresultatTilsynBarnDto,
    ): VedtakTilsynBarn {
        return VedtakTilsynBarn(
            behandlingId = behandling.id,
            type = TypeVedtak.INNVILGET,
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
