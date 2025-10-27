package no.nav.tilleggsstonader.sak.ekstern.googleRoutes

import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class GoogleRoutesClient(
    @Value("https://routes.googleapis.com/directions/v2:computeRoutes")
    private val baseUrl: URI,
    @Value("API-KEY") private val apiKey: String,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    private val uri = UriComponentsBuilder.fromUri(baseUrl).encode().toUriString()

    fun hentRuter(request: RuteRequest): RuteDto {
        val headers =
            HttpHeaders().apply {
                add("X-Goog-Api-Key", apiKey)
                add("X-Goog-FieldMask", "routes.duration,routes.distanceMeters")
                add("Content-Type", "application/json")
            }
        return postForEntity(uri, request, headers)
    }
}

data class RuteRequest(
    val origin: Address,
    val destination: Address,
)

data class Address(
    val address: String,
)

data class RuteDto(
    val routes: List<Route>?,
)

data class Route(
    val duration: String?,
    val distanceMeters: Int?,
)

fun main() {
    val client =
        GoogleRoutesClient(
            URI("https://routes.googleapis.com/directions/v2:computeRoutes"),
            "API-KEY",
            RestTemplate(),
        )

    val request =
        RuteRequest(
            origin = Address("1600 Amphitheatre Parkway, Mountain View, CA"),
            destination = Address("450 Serra Mall, Stanford, CA"),
        )

    val response = client.hentRuter(request)
    println("Ruteinfo fra routs-api: $response")
}
