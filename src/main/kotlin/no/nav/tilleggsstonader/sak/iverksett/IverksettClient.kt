package no.nav.tilleggsstonader.sak.iverksett

import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.BeriketSimuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDto
import no.nav.tilleggsstonader.sak.util.medContentTypeJsonUTF8
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI

@Component
class IverksettClient(
    @Value("\${clients.iverksett.uri}")
    private val familieEfIverksettUri: String,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) {

    fun simuler(simuleringRequest: SimuleringDto): BeriketSimuleringsresultat {
        val url = URI.create("$familieEfIverksettUri/api/simulering")

        return restOperations.exchange<BeriketSimuleringsresultat>(
            url,
            HttpMethod.POST,
            HttpEntity(simuleringRequest, HttpHeaders().medContentTypeJsonUTF8()),
        ).body ?: error("Mangler body")
    }
}
