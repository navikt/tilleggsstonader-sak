package no.nav.tilleggsstonader.sak.integrasjonstest

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.GodkjentGjennomførtKjøring
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus

/**
 * Gjennomfører en kjørelistebehandling uten avvik (alle uker auto-godkjennes).
 * Bruk denne når kjørelistene ikke inneholder avvik (parkeringsutgift ≤ 100 kr, ingen helg-kjøring, reisedager innenfor ramme).
 */
fun IntegrationTest.gjennomførKjørelisteBehandlingAutomatisk(
    behandling: Behandling,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
) = gjennomførKjørelisteBehandlingFelles(behandling, tilSteg, løsAvvikManuelt = false)

/**
 * Gjennomfører en kjørelistebehandling med manuell avviksbehandling.
 * Alle uker med avvik godkjennes av saksbehandler (GodkjentGjennomførtKjøring.JA / NEI basert på harKjørt).
 */
fun IntegrationTest.gjennomførKjørelisteBehandlingManuelt(
    behandling: Behandling,
    tilSteg: StegType = StegType.BEHANDLING_FERDIGSTILT,
) = gjennomførKjørelisteBehandlingFelles(behandling, tilSteg, løsAvvikManuelt = true)

private fun IntegrationTest.gjennomførKjørelisteBehandlingFelles(
    behandling: Behandling,
    tilSteg: StegType,
    løsAvvikManuelt: Boolean,
) {
    require(behandling.type == BehandlingType.KJØRELISTE) {
        "Behandling er ikke en kjøreliste-behandling"
    }

    val oppdatertBehandling = testoppsettService.hentBehandling(behandling.id)
    if (oppdatertBehandling.status == BehandlingStatus.FERDIGSTILT || oppdatertBehandling.steg == StegType.BEHANDLING_FERDIGSTILT) {
        return
    }

    // For at behandling skal oppdatere status OPPRETTET -> UTREDES. Henter også personpplysninger som trengs for å produsere internt vedtak
    // Vil alltid hentes fra frontend, så OK at vi gjør GET-kallet her selv uten at vi gjør noe med responsen
    kall.behandling.hent(behandling.id)
    tilordneÅpenBehandlingOppgaveForBehandling(behandling.id)

    if (løsAvvikManuelt) {
        løsAvvikUkerManuelt(behandling.id)
    }

    if (tilSteg == StegType.KJØRELISTE) return
    gjennomførKjørelisteSteg(behandling.id)

    if (tilSteg == StegType.BEREGNING) return
    gjennomførBeregningStegDagligReise(behandling.id)

    if (tilSteg == StegType.SIMULERING) return
    gjennomførSimuleringSteg(behandling.id)
    kall.privatBil.genererKjørelisteVedtaksbrev(behandling.id)
    kall.privatBil.fullførKjørelisteBehandling(behandling.id)

    if (tilSteg in setOf(StegType.JOURNALFØR_OG_DISTRIBUER_VEDTAKSBREV, StegType.FERDIGSTILLE_BEHANDLING)) return
    kjørTasksKlareForProsesseringTilIngenTasksIgjen()
}

private fun IntegrationTest.løsAvvikUkerManuelt(behandlingId: BehandlingId) {
    val reisevurderinger = kall.privatBil.hentReisevurderingForBehandling(behandlingId)

    reisevurderinger.forEach { reisevurdering ->
        reisevurdering.uker
            .filter { it.status == UkeStatus.AVVIK && it.avklartUkeId != null }
            .forEach { uke ->
                val avklarteDager =
                    uke.dager.map { dag ->
                        EndreAvklartDagRequest(
                            dato = dag.dato,
                            godkjentGjennomførtKjøring =
                                if (dag.kjørelisteDag?.harKjørt == true) {
                                    GodkjentGjennomførtKjøring.JA
                                } else {
                                    GodkjentGjennomførtKjøring.NEI
                                },
                            parkeringsutgift = dag.kjørelisteDag?.parkeringsutgift,
                            begrunnelse = "Godkjent manuelt i test",
                        )
                    }
                kall.privatBil.oppdaterUke(behandlingId, uke.avklartUkeId!!, avklarteDager)
            }
    }
}
