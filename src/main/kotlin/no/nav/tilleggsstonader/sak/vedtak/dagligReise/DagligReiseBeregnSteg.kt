package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.OpprettAndelerDagligReiseService
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregnSteg(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val simuleringService: SimuleringService,
    private val opprettAndelerDagligReiseService: OpprettAndelerDagligReiseService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        nullstillEksisterendeAndelerPåBehandling(saksbehandling)
        opprettAndeler(saksbehandling)
    }

    private fun opprettAndeler(saksbehandling: Saksbehandling) {
        opprettAndelerDagligReiseService.lagreAndelerForBehandling(saksbehandling)
    }

    private fun nullstillEksisterendeAndelerPåBehandling(saksbehandling: Saksbehandling) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
    }

    override fun stegType(): StegType = StegType.BEREGNING
}
