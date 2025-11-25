package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.migrering.arena.ArenaFinnesPersonRequest
import no.nav.tilleggsstonader.sak.migrering.arena.ArenaFinnesPersonResponse

class ArenaKall(
    private val testklient: Testklient,
) {
    fun status(dto: ArenaFinnesPersonRequest) = apiRespons.status(dto).expectOkWithBody<ArenaFinnesPersonResponse>()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = ArenaApi()

    inner class ArenaApi {
        fun status(dto: ArenaFinnesPersonRequest) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/ekstern/arena/status")
                    .body(dto)
                    .medClientCredentials(EksternApplikasjon.ARENA.namespaceAppNavn, true)
                    .exchange()
            }
    }
}
