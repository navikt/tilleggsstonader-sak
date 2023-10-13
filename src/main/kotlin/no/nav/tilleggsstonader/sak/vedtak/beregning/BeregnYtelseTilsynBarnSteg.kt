package no.nav.tilleggsstonader.sak.vedtak.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.beregning.dto.BeløpsperioderTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.beregning.dto.InnvilgelseTilsynBarnDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeregnYtelseTilsynBarnSteg(
    private val beregningTilsynBarnService: BeregningTilsynBarnService,
    vedtakService: VedtakService,
    tilkjentytelseService: TilkjentYtelseService,
    simuleringService: SimuleringService
) : BeregnYtelseSteg<InnvilgelseTilsynBarnDto>(
    stønadstype = Stønadstype.BARNETILSYN,
    vedtakService = vedtakService,
    tilkjentytelseService = tilkjentytelseService,
    simuleringService = simuleringService
) {

    override fun lagreVedtak(saksbehandling: Saksbehandling, data: InnvilgelseTilsynBarnDto) {
        val beløpsperioder = beregningTilsynBarnService.beregn(data)
        lagreVedtak(data)
        lagreAndeler(saksbehandling, data, beløpsperioder)
        /*
         Simulering burde kanskje kun gjøres når man går inn på fanen for simulering,
         og ikke i dette steget for å unngå feil fra simulering
         */
    }

    private fun lagreAndeler(
        saksbehandling: Saksbehandling,
        data: InnvilgelseTilsynBarnDto,
        beløpsperioder: List<BeløpsperioderTilsynBarnDto>
    ) {
        // Burde vi lagre beløpsperioder? Kan man kanskje lagre det som en del av vedtaket?
        val andelerTilkjentYtelse = emptyList<AndelTilkjentYtelse>()
        tilkjentytelseService.opprettTilkjentYtelse(
            TilkjentYtelse(
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                startdato = beregnStartdato(saksbehandling, andelerTilkjentYtelse)
            )
        )
    }

    private fun lagreVedtak(data: InnvilgelseTilsynBarnDto) {
        val vedtak = VedtakTilsynBarn(data.behandlingId, data.perioder)
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