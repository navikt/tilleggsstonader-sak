package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class FullmaktClient(
    @Value("\${clients.integrasjoner.uri}") private val baseUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    private val uriFullmektige = UriComponentsBuilder.fromUri(baseUrl)
        .pathSegment("api", "fullmakt", "fullmektige")
        .encode().toUriString()

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> {
        return postForEntity(uriFullmektige, IdentRequest(fullmaktsgiversIdent))
    }
}

// TODO: Legg i kontrakter
data class FullmektigDto(
    val fullmektigIdent: String,
    val fullmektigNavn: String? = null,
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val temaer: List<String>,
)
