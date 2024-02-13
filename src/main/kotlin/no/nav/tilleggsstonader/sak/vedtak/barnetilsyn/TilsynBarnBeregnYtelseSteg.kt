package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.LocalDate

@Service
class TilsynBarnBeregnYtelseSteg(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    private val barnService: BarnService,
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
        val beregningsresultat = tilsynBarnBeregningService.beregn(vedtak.stønadsperioder, vedtak.utgifter)
        validerBarnFinnesPåBehandling(saksbehandling, vedtak)
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

    private fun validerBarnFinnesPåBehandling(saksbehandling: Saksbehandling, vedtak: InnvilgelseTilsynBarnDto) {
        val barnPåBehandling = barnService.finnBarnPåBehandling(saksbehandling.id).map { it.id }.toSet()
        val barnSomIkkeFinnesPåBehandling = vedtak.utgifter.keys.filter { !barnPåBehandling.contains(it) }
        feilHvis(barnSomIkkeFinnesPåBehandling.isNotEmpty()) {
            "Det finnes utgifter på barn som ikke finnes på behandlingen, id=$barnSomIkkeFinnesPåBehandling"
        }
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatTilsynBarnDto,
    ) {
        val andelerTilkjentYtelse = beregningsresultat.perioder.flatMap {
            it.grunnlag.stønadsperioder.map { stønadsperiode ->
                AndelTilkjentYtelse(
                    // TODO hvordan burde vi egentligen gjøre med decimaler?
                    beløp = it.dagsats.setScale(0, RoundingMode.HALF_UP).toInt(),
                    fom = stønadsperiode.fom,
                    tom = stønadsperiode.tom,
                    satstype = Satstype.DAG, // TODO
                    type = TypeAndel.TILSYN_BARN_AAP, // TODO
                    kildeBehandlingId = saksbehandling.id,
                )
            }
        }.toSet()
        tilkjentytelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                startdato = beregnStartdato(saksbehandling, andelerTilkjentYtelse),
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
                stønadsperioder = vedtak.stønadsperioder,
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
}
