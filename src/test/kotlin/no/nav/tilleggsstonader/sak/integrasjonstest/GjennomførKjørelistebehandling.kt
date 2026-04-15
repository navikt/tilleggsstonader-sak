package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling

fun IntegrationTest.gjennomførKjørelisteBehandling(
    behandling: Behandling,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
) {
    require(behandling.type == BehandlingType.KJØRELISTE) {
        "Behandling er ikke en kjøreliste-behandling"
    }
    testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.UTREDES))
    tilordneÅpenBehandlingOppgaveForBehandling(behandling.id)

    if (tilSteg == StegType.KJØRELISTE) return
    gjennomførKjørelisteSteg(behandling.id)

    if (tilSteg == StegType.BEREGNING) return
    gjennomførBeregningStegDagligReise(behandling.id)

    if (tilSteg == StegType.SIMULERING) return
    gjennomførSimuleringSteg(behandling.id)
    kall.privatBil.fullførKjørelisteBehandling(behandling.id)

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) return
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()
}
