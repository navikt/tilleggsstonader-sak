package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingDto
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingResponse
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.sjekkRoutingForPerson(søknadRoutingDto: SøknadRoutingDto) =
    webTestClient
        .post()
        .uri("/api/ekstern/routing-soknad")
        .bodyValue(søknadRoutingDto)
        .medClientCredentials(
            clientId = EksternApplikasjon.SOKNAD_API.namespaceAppNavn,
            accessAsApplication = true,
        ).exchange()
        .expectStatus()
        .isOk()
        .expectBody<SøknadRoutingResponse>()
        .returnResult()
        .responseBody!!
