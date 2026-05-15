package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling

fun IntegrationTest.gjennomførKjørelisteBehandling(
    behandling: Behandling,
    tilSteg: StegType? = StegType.FULLFØR_KJØRELISTE,
) {
    require(behandling.type == BehandlingType.KJØRELISTE) {
        "Behandling er ikke en kjøreliste-behandling"
    }

    // For at behandling skal oppdatere status OPPRETTET -> UTREDES. Henter også personpplysninger som trengs for å produsere internt vedtak
    // Vil alltid hentes fra frontend, så OK at vi gjør GET-kallet her selv uten at vi gjør noe med responsen
    kall.behandling.hent(behandling.id)
    tilordneÅpenBehandlingOppgaveForBehandling(behandling.id)

    if (tilSteg == StegType.KJØRELISTE) return
    gjennomførKjørelisteSteg(behandling.id)

    if (tilSteg == StegType.BEREGNING) return
    gjennomførBeregningStegDagligReise(behandling.id)

    if (tilSteg == StegType.SIMULERING) return
    gjennomførSimuleringSteg(behandling.id)

    // Sjekk faktisk steg etter simulering — avhenger av toggle for totrinnskontroll
    val stegEtterSimulering = kall.behandling.hent(behandling.id).steg

    if (stegEtterSimulering == StegType.SEND_TIL_BESLUTTER) {
        if (tilSteg == StegType.SEND_TIL_BESLUTTER) return
        kall.privatBil.genererKjørelisteVedtaksbrev(behandling.id)
        kall.totrinnskontroll.sendTilBeslutter(behandling.id)

        if (tilSteg == StegType.BESLUTTE_VEDTAK) return
        // BeslutteVedtakSteg tar seg av resten når vedtaket besluttes
        gjennomførBeslutteVedtakSteg(behandling.id)
    } else {
        if (tilSteg in setOf(StegType.SEND_TIL_BESLUTTER, StegType.BESLUTTE_VEDTAK)) return
        // if (tilSteg == StegType.FULLFØR_KJØRELISTE) return
        kall.privatBil.fullførKjørelisteBehandling(behandling.id)
    }

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) return
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()
}
