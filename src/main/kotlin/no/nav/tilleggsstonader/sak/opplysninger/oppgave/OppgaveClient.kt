package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.medContentTypeJsonUTF8
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class OppgaveClient(
    @Value("\${clients.integrasjoner.uri}") private val integrasjonerBaseUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    private val oppgaveUri =
        UriComponentsBuilder.fromUri(integrasjonerBaseUrl).pathSegment("api/oppgave").build().toUri()

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): Long {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri).pathSegment("opprett").toUriString()

        return postForEntity<OppgaveResponse>(uri, opprettOppgave, HttpHeaders().medContentTypeJsonUTF8())
            .oppgaveId
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri).pathSegment("{id}").encode().toUriString()

        return getForEntity<Oppgave>(uri, uriVariables = oppgaveIdUriVariables(oppgaveId))
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri).pathSegment("finn").toUriString()

        return postForEntity<FinnOppgaveResponseDto>(
            uri,
            finnOppgaveRequest,
            HttpHeaders().medContentTypeJsonUTF8(),
        )
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String?, versjon: Int): Oppgave {
        val uriBuilder = UriComponentsBuilder.fromUri(oppgaveUri)
            .pathSegment("{id}", "fordel")
        val uriVariables = oppgaveIdUriVariables(oppgaveId).toMutableMap()

        val listOf = listOf("saksbehandler" to saksbehandler, "versjon" to versjon.toString())
        listOf.forEach { (key, value) ->
            if (value != null) {
                uriBuilder.queryParam(key, "{$key}")
                uriVariables[key] = value
            }
        }

        try {
            return postForEntity<Oppgave>(
                uriBuilder.encode().toUriString(),
                HttpHeaders().medContentTypeJsonUTF8(),
                uriVariables = uriVariables,
            )
        } catch (e: ProblemDetailException) {
            if (e.detail.detail?.contains("allerede er ferdigstilt") == true) {
                throw ApiFeil(
                    "Oppgaven med id=$oppgaveId er allerede ferdigstilt. Prøv å hente oppgaver på nytt.",
                    HttpStatus.BAD_REQUEST,
                )
            } else if (e.httpStatus == HttpStatus.CONFLICT) {
                throw ApiFeil(
                    "Oppgaven har endret seg siden du sist hentet oppgaver. For å kunne gjøre endringer må du hente oppgaver på nytt.",
                    HttpStatus.CONFLICT,
                )
            }
            throw e
        }
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri).pathSegment("{id}", "ferdigstill").encode().toUriString()
        patchForEntity<OppgaveResponse>(uri, "", uriVariables = oppgaveIdUriVariables(oppgaveId))
    }

    fun oppdaterOppgave(oppgave: Oppgave): Long {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri).pathSegment("{id}", "oppdater").encode().toUriString()
        try {
            val response = patchForEntity<OppgaveResponse>(
                uri,
                oppgave,
                HttpHeaders().medContentTypeJsonUTF8(),
                oppgaveIdUriVariables(oppgave.id),
            )
            return response.oppgaveId
        } catch (e: ProblemDetailException) {
            if (e.httpStatus == HttpStatus.CONFLICT) {
                throw ApiFeil(
                    "Oppgaven har endret seg siden du sist hentet oppgaver. For å kunne gjøre endringer må du laste inn siden på nytt",
                    HttpStatus.CONFLICT,
                )
            }
            throw e
        }
    }

    fun finnMapper(enhetsnummer: String, limit: Int): FinnMappeResponseDto {
        val uri = UriComponentsBuilder.fromUri(oppgaveUri)
            .pathSegment("mappe", "sok")
            .queryParam("enhetsnr", "{enhetsnr}")
            .queryParam("limit", "{limit}")
            .encode()
            .toUriString()
        val uriVariables = mapOf("enhetsnr" to enhetsnummer, "limit" to limit)
        return getForEntity<FinnMappeResponseDto>(uri, uriVariables = uriVariables)
    }

    private fun oppgaveIdUriVariables(oppgaveId: Long): Map<String, String> = mapOf("id" to oppgaveId.toString())
}
