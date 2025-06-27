package no.nav.tilleggsstonader.sak.opplysninger.ansvarligSaksbehandler

import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class AnsvarligSaksbehandlerClient(
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

        return getForEntity<Saksbehandler>(uri, null, uriVariables)
    }
}
