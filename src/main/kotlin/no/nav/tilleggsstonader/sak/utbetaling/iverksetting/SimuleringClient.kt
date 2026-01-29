package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.SimuleringDto
import no.nav.tilleggsstonader.sak.util.EnvUtil
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class SimuleringClient(
    @Value("\${clients.simulering.uri}") private val uri: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    fun simuler(simuleringRequest: SimuleringDto): SimuleringResponseDto? {
        val url =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api", "simulering", "v3")
                .toUriString()

        return try {
            postForEntityNullable<SimuleringResponseDto>(url, simuleringRequest)
        } catch (e: HttpClientErrorException.NotFound) {
            brukerfeilHvis(EnvUtil.erIDev() && e.responseBodyAsString.contains("Personen finnes ikke i PDL")) {
                "Simulering finner ikke personen i PDL. Prøv å gjenopprette personen i Dolly og prøv på nytt."
            }
            throw e
        }
    }
}
