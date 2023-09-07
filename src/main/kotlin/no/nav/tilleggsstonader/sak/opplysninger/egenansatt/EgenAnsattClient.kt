package no.nav.tilleggsstonader.sak.opplysninger.egenansatt

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EgenAnsattRestClient(
    @Value("\${clients.egen_ansatt.uri}") private val uri: URI,
    @Qualifier("utenAuth") private val restOperations: RestOperations,
) {

    private val egenAnsattUri: URI = UriComponentsBuilder.fromUri(uri).pathSegment("skjermet").build().toUri()
    private val egenAnsattBulkUri: URI = UriComponentsBuilder.fromUri(uri).pathSegment("skjermetBulk").build().toUri()

    fun erEgenAnsatt(personIdent: String): Boolean =
        restOperations.exchange<Boolean>(
            egenAnsattUri,
            HttpMethod.POST,
            HttpEntity(SkjermetDataRequestDTO(personIdent)),
        ).body ?: error("Mangler body")

    fun erEgenAnsatt(personidenter: Set<String>): Map<String, Boolean> =
        restOperations.exchange<Map<String, Boolean>>(
            egenAnsattBulkUri,
            HttpMethod.POST,
            HttpEntity(SkjermetDataBolkRequestDTO(personidenter)),
        ).body ?: error("Mangler body")
}

data class SkjermetDataRequestDTO(val personident: String)
data class SkjermetDataBolkRequestDTO(val personidenter: Set<String>)
