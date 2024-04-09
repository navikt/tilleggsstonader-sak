package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.BeriketSimuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.UUID

@Service
class IverksettClient(
    @Value("\${clients.iverksetting.uri}") private val uri: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val iverksettingUrl = UriComponentsBuilder.fromUri(uri)
        .pathSegment("api", "iverksetting", "tilleggsstonader")
        .encode().toUriString()

    fun iverksett(dto: IverksettDto) {
        try {
            postForEntityNullable<Void>(iverksettingUrl, dto)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                logger.warn("Iverksetting returnerte Conflict, ignorerer")
                return
            }
            throw e
        }
    }

    fun hentStatus(eksternFagsakId: Long, eksternBehandlingId: Long, iverksettingId: UUID): IverksettStatus {
        val url = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api", "iverksetting", "{sakId}", "{behandlingId}", "{iverksettingId}", "status")
            .encode().toUriString()
        val uriVariables = mapOf(
            "sakId" to eksternFagsakId,
            "behandlingId" to eksternBehandlingId,
            "iverksettingId" to iverksettingId,
        )
        return getForEntity<IverksettStatus>(url, uriVariables = uriVariables)
    }

    // TODO oppdater url når dp-iverksett har støtte for simulering
    fun simuler(simuleringRequest: SimuleringDto): BeriketSimuleringsresultat {
        val url = UriComponentsBuilder.fromUri(uri)
            .pathSegment("api", "simulering")
            .toUriString()

        return postForEntity<BeriketSimuleringsresultat>(url, simuleringRequest)
    }
}
