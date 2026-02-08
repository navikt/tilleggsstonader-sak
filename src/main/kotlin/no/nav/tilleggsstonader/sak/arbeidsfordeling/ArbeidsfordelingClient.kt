package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.libs.http.client.getForEntity
import no.nav.tilleggsstonader.libs.http.client.postForEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class ArbeidsfordelingClient(
    @Value("\${clients.norg2.uri}")
    private val uri: URI,
    @Qualifier("utenAuth") private val restTemplate: RestTemplate,
) {
    fun finnArbeidsfordelingsenhet(arbeidsfordelingskriterie: ArbeidsfordelingKriterie): List<Arbeidsfordelingsenhet> {
        val bestMatchUri =
            UriComponentsBuilder
                .fromUri(arbeidsfordelingUri)
                .pathSegment("enheter/bestmatch")
                .build()
                .toUriString()

        return restTemplate.postForEntity(bestMatchUri, arbeidsfordelingskriterie)
    }

    fun finnNavKontorForGeografiskOmråde(
        geografiskOmraade: String?,
        diskresjonskode: String?,
        erEgenAnsatt: Boolean,
    ): NavKontor? {
        val uri =
            UriComponentsBuilder
                .fromUri(enhetUri)
                .pathSegment("navkontor/{geografiskOmraade}")
                .queryParam("disk", "{disk}")
                .queryParam("skjermet", "{skjermet}")
                .build()
                .toUriString()

        return restTemplate.getForEntity<NavKontor>(
            uri = uri,
            uriVariables =
                mapOf(
                    "geografiskOmraade" to geografiskOmraade,
                    "disk" to (diskresjonskode ?: "ANY"),
                    "skjermet" to erEgenAnsatt,
                ),
        )
    }

    private val arbeidsfordelingUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/v1/arbeidsfordeling")
            .build()
            .toUri()

    private val enhetUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api/v1/enhet")
            .build()
            .toUri()
}
