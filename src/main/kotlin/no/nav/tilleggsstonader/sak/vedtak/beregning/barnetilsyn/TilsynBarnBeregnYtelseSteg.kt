package no.nav.tilleggsstonader.sak.vedtak.beregning.barnetilsyn

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.beregning.BeregnYtelseSteg
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilsynBarnBeregnYtelseSteg(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    vedtakService: VedtakService,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<InnvilgelseTilsynBarnDto>(
    stønadstype = Stønadstype.BARNETILSYN,
    vedtakService = vedtakService,
    tilkjentytelseService = tilkjentytelseService,
    simuleringService = simuleringService,
) {

    override fun lagreVedtak(saksbehandling: Saksbehandling, data: InnvilgelseTilsynBarnDto) {
        val beregningsresultat = tilsynBarnBeregningService.beregn(data)
        lagreVedtak(data)
        lagreAndeler(saksbehandling, data, beregningsresultat)
        /*
        Funksjonalitet som mangler:
         * Avslag
         * Revurdering
         * Opphør

         Simulering burde kanskje kun gjøres når man går inn på fanen for simulering,
         og ikke i dette steget for å unngå feil fra simulering
         */
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseTilsynBarnDto,
        beregningsresultat: BeregningsresultatTilsynBarnDto,
    ) {
        // Burde vi lagre beløpsperioder? Kan man kanskje lagre det som en del av vedtaket?
        val andelerTilkjentYtelse = emptyList<AndelTilkjentYtelse>()
        tilkjentytelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                startdato = beregnStartdato(saksbehandling, andelerTilkjentYtelse),
            ),
        )
    }

    private fun lagreVedtak(data: InnvilgelseTilsynBarnDto) {
        // validere at barnen finns på behandlingen
        val vedtak = VedtakTilsynBarn(data.behandlingId, data.perioder, emptyList())
        vedtakService.lagreVedtak(vedtak)
    }

    /**
     * Vi lagrer ned startdato for å eventuellt kunne opphøre bak i tiden for perioder som Arena eier.
     */
    private fun beregnStartdato(
        saksbehandling: Saksbehandling,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    ): LocalDate {
        feilHvis(saksbehandling.forrigeBehandlingId != null) {
            "Når vi begynner å revurdere og opphøre må vi oppdatere denne metoden for å finne startdato"
        }
        return minOf(andelerTilkjentYtelse.minOf { it.stønadFom })
    }
}
