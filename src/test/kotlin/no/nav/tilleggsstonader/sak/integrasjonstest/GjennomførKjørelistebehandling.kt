package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling

fun IntegrationTest.gjennomførKjørelisteBehandling(
    behandling: Behandling,
    tilSteg: StegType? = StegType.BEHANDLING_FERDIGSTILT,
) {
    require(behandling.type == BehandlingType.KJØRELISTE) {
        "Behandling er ikke en kjøreliste-behandling"
    }

    // For at behandling skal oppdatere status OPPRETTET -> UTREDES. Henter også personpplysninger som trengs for å produsere internt vedtak
    // Vil alltid hentes fra frontend, så OK at vi gjør GET-kallet her selv uten at vi gjør noe med responsen
    tilordneÅpenBehandlingOppgaveForBehandling(behandling.id)
    val behandlingDto = kall.behandling.hent(behandling.id)
    var nesteSteg = behandling.steg
    while (nesteSteg != tilSteg) {
        // Må generere kjørelistebrev før behandling blir sendt til beslutter
        if (nesteSteg == StegType.SEND_TIL_BESLUTTER) {
            kall.privatBil.genererKjørelisteVedtaksbrev(behandling.id)
        }
        nesteSteg = utførStegOgReturnerNesteSteg(nesteSteg, behandlingDto)
    }

    kjørTasksKlareForProsesseringTilIngenTasksIgjen()
}
