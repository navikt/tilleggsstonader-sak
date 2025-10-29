package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringRequestDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.SimuleringDto
import no.nav.tilleggsstonader.sak.util.EnvUtil
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

    private val iverksettingUrl =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("api", "iverksetting", "v2")
            .encode()
            .toUriString()

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

    fun hentStatus(
        eksternFagsakId: Long,
        eksternBehandlingId: Long,
        iverksettingId: UUID,
    ): IverksettStatus {
        val url =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api", "iverksetting", "{sakId}", "{behandlingId}", "{iverksettingId}", "status")
                .encode()
                .toUriString()
        val uriVariables =
            mapOf(
                "sakId" to eksternFagsakId,
                "behandlingId" to eksternBehandlingId,
                "iverksettingId" to iverksettingId,
            )
        return getForEntity<IverksettStatus>(url, uriVariables = uriVariables)
    }

    fun simulerV2(simuleringRequest: SimuleringRequestDto): SimuleringResponseDto? {
        val url =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api", "simulering", "v2")
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

    fun simulerV3(simuleringRequest: Collection<SimuleringDto>): SimuleringResponseDto? {
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
