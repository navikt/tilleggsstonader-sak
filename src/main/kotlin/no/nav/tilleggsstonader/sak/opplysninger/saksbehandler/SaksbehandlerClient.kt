package no.nav.tilleggsstonader.sak.opplysninger.saksbehandler

import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SaksbehandlerClient(
    @Value("\${clients.integrasjoner.uri}") private val integrasjonerBaseUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    private val saksbehandlerUri =
        UriComponentsBuilder
            .fromUri(integrasjonerBaseUrl)
            .pathSegment("api/saksbehandler")
            .build()
            .toUri()

    fun hentSaksbehandlerInfo(navIdent: String): Saksbehandler {
        val uri =
            UriComponentsBuilder
                .fromUri(saksbehandlerUri)
                .pathSegment("{id}")
                .encode()
                .toUriString()
        val uriVariables =
            mapOf(
                "id" to navIdent,
            )

        return kastBrukerFeilHvisBadRequest { getForEntity<Saksbehandler>(uri, null, uriVariables) }
    }

    private fun <T> kastBrukerFeilHvisBadRequest(fn: () -> T): T =
        try {
            fn()
        } catch (e: ProblemDetailException) {
            val detail = e.detail.detail
            brukerfeilHvis(e.httpStatus == HttpStatus.BAD_REQUEST && detail != null) {
                detail ?: "Ukjent feil ved henting av saksbehandler"
            }
            throw e
        }
}
