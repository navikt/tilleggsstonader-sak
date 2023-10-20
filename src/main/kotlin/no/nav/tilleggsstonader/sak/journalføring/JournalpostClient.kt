package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostResponse
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.libs.log.NavHttpHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class JournalpostClient(
    @Value("\${clients.integrasjoner.uri}") private val integrasjonerBaseUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    fun hentJournalpost(journalpostId: String): Journalpost =
        getForEntity<Journalpost>(journalpostUri.toString(), uriVariables = journalpostIdUriVariables(journalpostId))

    private val journalpostUri =
        UriComponentsBuilder.fromUri(integrasjonerBaseUrl).pathSegment("api/journalpost").build().toUri()
    private val dokarkivUri =
        UriComponentsBuilder.fromUri(integrasjonerBaseUrl).pathSegment("api/dokarkiv").build().toUri()


    fun oppdaterJournalpost(
        oppdaterJournalpostRequest: OppdaterJournalpostRequest,
        journalpostId: String,
        saksbehandler: String?,
    ): OppdaterJournalpostResponse {
        return putForEntity<OppdaterJournalpostResponse>(
            URI.create("$dokarkivUri/$journalpostId").toString(),
            oppdaterJournalpostRequest,
            headerMedSaksbehandler(saksbehandler),
        )
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String, saksbehandler: String?): OppdaterJournalpostResponse {
        return  putForEntity<OppdaterJournalpostResponse>("$dokarkivUri/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet",
            "",
            headerMedSaksbehandler(saksbehandler),
        )
    }


    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        if (saksbehandler != null) {
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }

    private fun oppgaveIdUriVariables(oppgaveId: Long): Map<String, String> = mapOf("id" to oppgaveId.toString())
    private fun journalpostIdUriVariables(journalpostId: String): Map<String, String> = mapOf("journalpostId" to journalpostId)
}
