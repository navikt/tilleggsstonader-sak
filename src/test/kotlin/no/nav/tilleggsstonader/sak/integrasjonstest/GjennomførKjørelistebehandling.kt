package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling

fun IntegrationTest.gjennomførKjørelisteBehandling(behandling: Behandling) {
    require(behandling.type == BehandlingType.KJØRELISTE) {
        "Behandling er ikke en kjøreliste-behandling"
    }
    testoppsettService.oppdater(behandling.copy(status = BehandlingStatus.UTREDES))
    tilordneÅpenBehandlingOppgaveForBehandling(behandling.id)
    gjennomførKjørelisteSteg(behandling.id)
    gjennomførBeregningStegDagligReise(behandling.id)
    gjennomførSimuleringSteg(behandling.id)
    kall.privatBil.fullførKjørelisteBehandling(behandling.id)
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()
}
