package no.nav.tilleggsstonader.sak.opplysninger.egenansatt

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EgenAnsattClient(
    @Value("\${clients.egen_ansatt.uri}") private val uri: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    private val egenAnsattUri: URI = UriComponentsBuilder.fromUri(uri).pathSegment("skjermet").build().toUri()
    private val egenAnsattBulkUri: URI = UriComponentsBuilder.fromUri(uri).pathSegment("skjermetBulk").build().toUri()

    fun erEgenAnsatt(personIdent: String): Boolean =
        restTemplate.exchange<Boolean>(
            egenAnsattUri,
            HttpMethod.POST,
            HttpEntity(SkjermetDataRequestDTO(personIdent)),
        ).body ?: error("Mangler body")

    fun erEgenAnsatt(personidenter: Set<String>): Map<String, Boolean> =
        restTemplate.exchange<Map<String, Boolean>>(
            egenAnsattBulkUri,
            HttpMethod.POST,
            HttpEntity(SkjermetDataBolkRequestDTO(personidenter)),
        ).body ?: error("Mangler body")
}

data class SkjermetDataRequestDTO(val personident: String)
data class SkjermetDataBolkRequestDTO(val personidenter: Set<String>)
