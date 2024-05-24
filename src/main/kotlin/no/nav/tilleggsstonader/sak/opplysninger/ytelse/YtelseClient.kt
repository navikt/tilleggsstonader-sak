package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class YtelseClient(
    @Value("\${clients.integrasjoner.uri}") private val baseUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    val uri = UriComponentsBuilder.fromUri(baseUrl)
        .pathSegment("api", "ytelse", "finn")
        .queryParam("fom", "{fom}")
        .queryParam("tom", "{tom}")
        .encode()
        .toUriString()

    fun hentYtelser(ident: String, fom: LocalDate, tom: LocalDate): YtelsePerioderDto {
        val uriVariables = mapOf<String, Any>(
            "fom" to fom,
            "tom" to tom,
        )

        return postForEntity(uri, IdentRequest(ident), uriVariables = uriVariables)
    }
}
