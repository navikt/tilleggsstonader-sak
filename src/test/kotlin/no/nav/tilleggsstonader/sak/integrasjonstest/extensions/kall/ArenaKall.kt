package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.migrering.arena.ArenaFinnesPersonRequest
import no.nav.tilleggsstonader.sak.migrering.arena.ArenaFinnesPersonResponse

class ArenaKall(
    private val test: IntegrationTest,
) {
    fun status(dto: ArenaFinnesPersonRequest): ArenaFinnesPersonResponse = statusResponse(dto).expectOkWithBody()

    fun statusResponse(dto: ArenaFinnesPersonRequest) =
        with(test) {
            webTestClient
                .post()
                .uri("/api/ekstern/arena/status")
                .bodyValue(dto)
                .medClientCredentials(EksternApplikasjon.ARENA.namespaceAppNavn, true)
                .exchange()
        }
}
