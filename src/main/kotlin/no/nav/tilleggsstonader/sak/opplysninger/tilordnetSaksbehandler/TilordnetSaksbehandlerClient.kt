package no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler

import no.nav.tilleggsstonader.kontrakter.felles.Saksbehandler
import no.nav.tilleggsstonader.libs.http.client.getForEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class TilordnetSaksbehandlerClient(
    @Value("\${clients.integrasjoner.uri}") private val integrasjonerBaseUrl: URI,
    @Qualifier("azure") private val restTemplate: RestTemplate,
) {
    private val saksbehandlerUri =
        UriComponentsBuilder
            .fromUri(integrasjonerBaseUrl)
            .pathSegment("api/saksbehandler")
            .build()
            .toUri()

    @Cacheable("saksbehandlerInfo", cacheManager = "longCache")
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

        return restTemplate.getForEntity<Saksbehandler>(uri, null, uriVariables)
    }
}
