package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.søk.Søkeresultat
import no.nav.tilleggsstonader.sak.infrastruktur.felles.PersonIdentDto

class PersonKall(
    private val test: IntegrationTest,
) {
    fun fagsakEkstern(eksternFagsakId: Long): Søkeresultat = fagsakEksternResponse(eksternFagsakId).expectOkWithBody()

    fun fagsakEksternResponse(eksternFagsakId: Long) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/sok/person/fagsak-ekstern/$eksternFagsakId")
                .medOnBehalfOfToken()
                .exchange()
        }

    fun sok(personIdent: String): Søkeresultat = sokResponse(personIdent).expectOkWithBody()

    fun sokResponse(personIdent: String) =
        with(test) {
            webTestClient
                .post()
                .uri("/api/sok")
                .bodyValue(PersonIdentDto(personIdent = personIdent))
                .medOnBehalfOfToken()
                .exchange()
        }
}
