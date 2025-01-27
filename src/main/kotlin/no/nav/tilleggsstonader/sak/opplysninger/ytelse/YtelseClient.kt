package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderRequest
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class YtelseClient(
    @Value("\${clients.integrasjoner.uri}") private val baseUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    val uri =
        UriComponentsBuilder
            .fromUri(baseUrl)
            .pathSegment("api", "ytelse", "finn")
            .encode()
            .toUriString()

    fun hentYtelser(request: YtelsePerioderRequest): YtelsePerioderDto = postForEntity(uri, request)
}
