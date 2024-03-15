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
    @Value("\${clients.integrasjoner.uri}") private val baseUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    /**
     * @param tom Default: 60 dager frem i tid.
     */
    fun hentAktiviteter(ident: String, fom: LocalDate, tom: LocalDate?): List<AktivitetArenaDto> {
        val uriVariables = mutableMapOf<String, Any>("fom" to fom)

        val uriBuilder = UriComponentsBuilder.fromUri(baseUrl)
            .pathSegment("api", "aktivitet", "finn")
            .queryParam("fom", "{fom}")

        if (tom != null) {
            uriBuilder.queryParam("tom", "{tom}")
            uriVariables["tom"] = tom
        }

        return postForEntity(
            uriBuilder.encode().toUriString(),
            IdentRequest(ident),
            uriVariables = uriVariables,
        )
    }
}
