package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling

fun IntegrationTest.gjennomførKjørelisteBehandling(behandling: Behandling) {
    require(behandling.type == BehandlingType.KJØRELISTE) {
        "Behandling er ikke en kjøreliste-behandling"
    }
    tilordneÅpenBehandlingOppgaveForBehandling(behandling.id)
    // For at behandling skal oppdatere status OPPRETTET -> UTREDES. Henter også personpplysninger som trengs for å produsere internt vedtak
    // Vil alltid hentes fra frontend, så OK at vi gjør GET-kallet her selv uten at vi gjør noe med responsen
    kall.behandling.hent(behandling.id)
    gjennomførKjørelisteSteg(behandling.id)
    gjennomførBeregningStegDagligReise(behandling.id)
    gjennomførSimuleringSteg(behandling.id)
    kall.privatBil.fullførKjørelisteBehandling(behandling.id)
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()
}
