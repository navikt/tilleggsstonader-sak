package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregnSteg(
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

    private fun opprettAndeler(saksbehandling: Saksbehandling) {
        // TODO: Vurder å lage en egen vedtakService som henter vedtak på en penere måte
        // VedtakService kan ikke brukes fordi det fører til circle dependency
        val vedtak = vedtakRepository.findByIdOrThrow(saksbehandling.id).withTypeOrThrow<InnvilgelseDagligReise>()
        val beregningsresultatOffentligTransport = vedtak.data.beregningsresultat?.offentligTransport

        if (beregningsresultatOffentligTransport != null) {
            tilkjentYtelseService.lagreTilkjentYtelse(
                behandlingId = saksbehandling.id,
                andeler = beregningsresultatOffentligTransport.mapTilAndelTilkjentYtelse(saksbehandling),
            )
        }
    }

    private fun nullstillEksisterendeAndelerPåBehandling(saksbehandling: Saksbehandling) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
    }

    override fun stegType(): StegType = StegType.BEREGNING
}
