package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class ArbeidsfordelingClient(
    @Value("\${clients.integrasjoner.uri}") private val integrasjonerBaseUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    fun hentNavEnhetForPersonMedRelasjoner(ident: String): List<Arbeidsfordelingsenhet> {
        TODO("Not yet implemented")
    }

    fun hentBehandlendeEnhetForOppfølging(ident: String): Arbeidsfordelingsenhet? {
        TODO("Not yet implemented")
    }

    private val arbeidsFordelingUri =
        UriComponentsBuilder.fromUri(integrasjonerBaseUrl).pathSegment("api/arbeidsfordeling").build().toUri()
}