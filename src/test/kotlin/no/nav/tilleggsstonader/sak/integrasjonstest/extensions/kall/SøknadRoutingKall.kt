package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.IdentSkjematype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingResponse
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.sjekkRoutingForPerson(identSkjematype: IdentSkjematype) =
    webTestClient
        .post()
        .uri("/api/ekstern/skjema-routing")
        .bodyValue(identSkjematype)
        .medClientCredentials(
            clientId = EksternApplikasjon.SOKNAD_API.namespaceAppNavn,
            accessAsApplication = true,
        ).exchange()
        .expectStatus()
        .isOk()
        .expectBody<SøknadRoutingResponse>()
        .returnResult()
        .responseBody!!
