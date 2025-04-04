package no.nav.tilleggsstonader.sak.opplysninger.kodeverk

import no.nav.tilleggsstonader.kontrakter.kodeverk.KodeverkDto
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KodeverkClient(
    @Value("\${clients.kodeverk.uri}")
    private val uri: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    fun hentPostnummer(): KodeverkDto = getForEntity(kodeverkUri, null, mapOf("kodeverksnavn" to "Postnummer"))

    fun hentLandkoder(): KodeverkDto = getForEntity(kodeverkUri, null, mapOf("kodeverksnavn" to "Landkoder"))

    fun hentLandkoderIso2(): KodeverkDto = getForEntity(kodeverkUri, null, mapOf("kodeverksnavn" to "LandkoderISO2"))

    private val kodeverkUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api", "v1", "kodeverk", "{kodeverksnavn}", "koder", "betydninger")
            .queryParam("ekskluderUgyldige", "true") // henter ikke historikk
            .queryParam("spraak", "nb")
            .encode()
            .toUriString()
}
