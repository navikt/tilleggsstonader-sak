package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.søk.Søkeresultat
import no.nav.tilleggsstonader.sak.infrastruktur.felles.PersonIdentDto
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.søkPersonPåEksternFagsakIdKall(eksternFagsakId: Long) =
    webTestClient
        .get()
        .uri("/api/sok/person/fagsak-ekstern/$eksternFagsakId")
        .medOnBehalfOfToken()
        .exchange()

fun IntegrationTest.søkPersonPåEksternFagsakId(eksternFagsakId: Long) =
    søkPersonPåEksternFagsakIdKall(eksternFagsakId)
        .expectStatus()
        .isOk
        .expectBody<Søkeresultat>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.søkPersonKall(personIdent: String) =
    webTestClient
        .post()
        .uri("/api/sok")
        .bodyValue(PersonIdentDto(personIdent = personIdent))
        .medOnBehalfOfToken()
        .exchange()

fun IntegrationTest.søkPerson(personIdent: String) =
    søkPersonKall(personIdent)
        .expectStatus()
        .isOk
        .expectBody<Søkeresultat>()
        .returnResult()
        .responseBody!!
