package no.nav.tilleggsstonader.sak.behandling.opprettelse

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

sealed class OpprettRevurderingResultat {
    data class Opprettet(val behandlingId: BehandlingId) : OpprettRevurderingResultat()

    data object ÅpneBehandlingerFunnet : OpprettRevurderingResultat()
}
