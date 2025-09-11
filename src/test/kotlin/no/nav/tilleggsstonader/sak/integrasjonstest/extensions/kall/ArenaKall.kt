package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.migrering.arena.ArenaFinnesPersonRequest
import no.nav.tilleggsstonader.sak.migrering.arena.ArenaFinnesPersonResponse
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.hentStatusFraArenaKall(arenaFinnesPersonRequest: ArenaFinnesPersonRequest) =
    webTestClient
        .post()
        .uri("/api/ekstern/arena/status")
        .bodyValue(arenaFinnesPersonRequest)
        .medClientCredentials(EksternApplikasjon.ARENA.namespaceAppNavn, true)
        .exchange()

fun IntegrationTest.hentStatusFraArena(arenaFinnesPersonRequest: ArenaFinnesPersonRequest) =
    hentStatusFraArenaKall(arenaFinnesPersonRequest)
        .expectStatus()
        .isOk
        .expectBody<ArenaFinnesPersonResponse>()
        .returnResult()
        .responseBody!!
