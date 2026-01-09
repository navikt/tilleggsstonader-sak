package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseBeregnYtelseSteg(
    private val vedtakRepository: VedtakRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val simuleringService: SimuleringService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        nullstillEksisterendeAndelerPåBehandling(saksbehandling)
        opprettAndeler(saksbehandling)
    }

    override fun stegType(): StegType = StegType.BEREGNE_YTELSE

    private fun nullstillEksisterendeAndelerPåBehandling(saksbehandling: Saksbehandling) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
    }

    private fun opprettAndeler(saksbehandling: Saksbehandling) {
        val vedtak =
            vedtakRepository.findByIdOrThrow(saksbehandling.id).withTypeOrThrow<InnvilgelseDagligReise>()

        val beregningsresultat = vedtak.data.beregningsresultat

        val andeler =
            listOfNotNull(
                beregningsresultat?.privatBil?.mapTilAndelTilkjentYtelse(saksbehandling),
                beregningsresultat?.offentligTransport?.mapTilAndelTilkjentYtelse(saksbehandling),
            ).flatten()

        if (andeler.isNotEmpty()) {
            tilkjentYtelseService.lagreTilkjentYtelse(
                behandlingId = saksbehandling.id,
                andeler = andeler,
            )
        }
    }

    private fun BeregningsresultatPrivatBil.mapTilAndelTilkjentYtelse(saksbehandling: Saksbehandling): List<AndelTilkjentYtelse> {
        // Mock implementation for testing
        return reiser.flatMap { reise ->
            reise.uker.map { (grunnlag, stønadsbeløp) ->
                // TODO - skal stønadsbeløp være 0 siden rammen er et 0-vedtak?
                // TODO - hvilke datoer skal settes?
                AndelTilkjentYtelse(
                    beløp = 0,
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    satstype = Satstype.DAG,
                    type = TypeAndel.DAGLIG_REISE_AAP,
                    kildeBehandlingId = saksbehandling.id,
                    utbetalingsdato = LocalDate.now(),
                    brukersNavKontor = "Mock",
                )
            }
        }
    }
}
