package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostResponse
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.libs.log.NavHttpHeaders
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
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
        val uri = UriComponentsBuilder.fromUri(dokarkivUri).pathSegment("{journalpostId}").encode().toUriString()
        return putForEntity<OppdaterJournalpostResponse>(
            uri,
            oppdaterJournalpostRequest,
            headerMedSaksbehandler(saksbehandler),
            journalpostIdUriVariables(journalpostId),
        )
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String, saksbehandler: String?): OppdaterJournalpostResponse {
        val uri = UriComponentsBuilder.fromUri(dokarkivUri).pathSegment("{journalpostId}", "ferdigstill")
            .queryParam("journalfoerendeEnhet", { journalførendeEnhet }).encode().toUriString()

        return putForEntity<OppdaterJournalpostResponse>(
            uri,
            "",
            headerMedSaksbehandler(saksbehandler),
            journalpostIdUriVariables(journalpostId),
        )
    }

    fun hentSøknadTilsynBarn(journalpostId: String, dokumentId: String): SøknadBarnetilsyn {
        val data = getForEntity<ByteArray>(jsonDokumentUri(journalpostId, dokumentId).toString())
        return objectMapper.readValue<SøknadBarnetilsyn>(data)
    }

    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        if (saksbehandler != null) {
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }

    private fun jsonDokumentUri(journalpostId: String, dokumentInfoId: String): URI {
        return UriComponentsBuilder
            .fromUri(journalpostUri)
            .pathSegment("hentdokument", journalpostId, dokumentInfoId)
            .queryParam("variantFormat", Dokumentvariantformat.ORIGINAL)
            .build()
            .toUri()
    }

    private fun journalpostIdUriVariables(journalpostId: String): Map<String, String> = mapOf("journalpostId" to journalpostId)
}
