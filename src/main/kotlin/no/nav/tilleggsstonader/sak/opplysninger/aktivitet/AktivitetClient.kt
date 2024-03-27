package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
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
class AktivitetClient(
    @Value("\${clients.integrasjoner.uri}") private val uri: URI,
    @Qualifier("azureClientCredential")
    restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    fun hentAKtiviteter(ident: String, fom: LocalDate, tom: LocalDate): List<AktivitetArenaDto> {
        val uriVariables = mutableMapOf<String, Any>(
            "fom" to fom,
            "tom" to tom,
        )
        val aktivitetUri = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api", "aktivitet", "finn")
            .queryParam("fom", "{fom}")
            .queryParam("tom", "{tom}")
            .encode().toUriString()

        return postForEntity<List<AktivitetArenaDto>>(aktivitetUri, IdentRequest(ident), null, uriVariables)
    }
}
