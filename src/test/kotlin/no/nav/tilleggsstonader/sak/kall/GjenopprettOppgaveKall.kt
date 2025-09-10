package no.nav.tilleggsstonader.sak.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId

// Må kalles med utvikler-rettighet
fun IntegrationTest.gjenopprettOppgaveKall(behandlingId: BehandlingId) =
    webTestClient
        .post()
        .uri("/api/forvaltning/oppgave/gjenopprett/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()
