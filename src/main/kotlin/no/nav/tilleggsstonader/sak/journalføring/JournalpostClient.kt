package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostResponse
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.JournalposterForBrukerRequest
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

    private val journalpostUri =
        UriComponentsBuilder.fromUri(integrasjonerBaseUrl).pathSegment("api/journalpost").build().toUri()

    private val dokarkivUri =
        UriComponentsBuilder.fromUri(integrasjonerBaseUrl).pathSegment("api/arkiv").build().toUri()

    private val dokdistUri = UriComponentsBuilder.fromUri(integrasjonerBaseUrl).pathSegment("api/dist").build().toUri()

    fun finnJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        val uri = URI.create("$journalpostUri").toString()

        return postForEntity<List<Journalpost>>(uri, journalposterForBrukerRequest)
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        val uri = UriComponentsBuilder.fromUri(journalpostUri).queryParam("journalpostId", "{journalpostId}").encode()
            .toUriString()

        return getForEntity<Journalpost>(uri, uriVariables = journalpostIdUriVariables(journalpostId))
    }

    fun opprettJournalpost(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String?,
    ): ArkiverDokumentResponse {
        return postForEntity(dokarkivUri.toString(), arkiverDokumentRequest, headerMedSaksbehandler(saksbehandler))
    }

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

    fun ferdigstillJournalpost(
        journalpostId: String,
        journalførendeEnhet: String,
        saksbehandler: String?,
    ): OppdaterJournalpostResponse {
        val uri = UriComponentsBuilder.fromUri(dokarkivUri).pathSegment("{journalpostId}", "ferdigstill")
            .queryParam("journalfoerendeEnhet", "{journalfoerendeEnhet}").encode().toUriString()

        return putForEntity<OppdaterJournalpostResponse>(
            uri,
            "",
            headerMedSaksbehandler(saksbehandler),
            journalpostIdUriVariables(journalpostId) + journalførendeEnhetUriVariables(journalførendeEnhet),
        )
    }

    fun distribuerJournalpost(request: DistribuerJournalpostRequest, saksbehandler: String? = null): String {
        return postForEntity<String>(
            dokdistUri.toString(),
            request,
            headerMedSaksbehandler(saksbehandler),
        )
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        dokumentVariantformat: Dokumentvariantformat,
    ): ByteArray {
        // TODO: kastApiFeilDersomUtviklerMedVeilederrolle() for å ikke gi tilgang til dokumenter med feil tema i prod
        val uri = UriComponentsBuilder.fromUri(journalpostUri)
            .pathSegment("hentdokument", "{journalpostId}", "{dokumentInfoId}")
            .queryParam("variantFormat", dokumentVariantformat).encode().toUriString()

        return getForEntity<ByteArray>(uri, uriVariables = mapOf("journalpostId" to journalpostId, "dokumentInfoId" to dokumentInfoId))
    }

    fun oppdaterLogiskeVedlegg(dokumentInfoId: String, request: BulkOppdaterLogiskVedleggRequest): String {
        val uri = UriComponentsBuilder.fromUri(dokarkivUri).pathSegment("dokument", "{dokumentInfoId}", "logiskVedlegg")
            .encode().toUriString()

        return putForEntity<String>(
            uri,
            request,
            uriVariables = mapOf("dokumentInfoId" to dokumentInfoId),
        )
    }

    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        if (saksbehandler != null) {
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }

    private fun journalpostIdUriVariables(journalpostId: String): Map<String, String> =
        mapOf("journalpostId" to journalpostId)

    private fun journalførendeEnhetUriVariables(journalførendeEnhet: String): Map<String, String> =
        mapOf("journalfoerendeEnhet" to journalførendeEnhet)
}
