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
class RegisterAktivitetClient(
    @Value("\${clients.integrasjoner.uri}") private val baseUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    val uriAktiviteter =
        UriComponentsBuilder
            .fromUri(baseUrl)
            .pathSegment("api", "aktivitet", "finn")
            .queryParam("fom", "{fom}")
            .queryParam("tom", "{tom}")
            .encode()
            .toUriString()

    fun hentAktiviteter(
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<AktivitetArenaDto> {
        val uriVariables = mutableMapOf<String, Any>("fom" to fom, "tom" to tom)
        return postForEntity(uriAktiviteter, IdentRequest(ident), uriVariables = uriVariables)
    }
}
