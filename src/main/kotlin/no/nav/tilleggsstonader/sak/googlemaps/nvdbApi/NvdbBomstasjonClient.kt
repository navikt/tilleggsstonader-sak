package no.nav.tilleggsstonader.sak.googlemaps.nvdbApi

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class NvdbBomstasjonClient(
    @Value("\${clients.nvdb.uri}") private val baseUrl: String,
    builder: RestClient.Builder,
) {
    private val restClient =
        builder
            .baseUrl(baseUrl)
            .defaultHeader("X-Client", "nav-tilleggsstonader-sak")
            .build()

    fun hentAlleBomstasjoner(): List<NvdbBomstasjon> {
        val response = nvdbKall()
        return response.objekter.mapNotNull { it.tilDomene() }
    }

    private fun nvdbKall(): NvdbBomstasjonResponse =
        restClient
            .get()
            .uri { builder ->
                builder
                    .path("/vegobjekter/45")
                    .queryParam("inkluder", "metadata,egenskaper,lokasjon")
                    .queryParam("srid", "4326")
                    .build()
            }.retrieve()
            .body<NvdbBomstasjonResponse>()!!
}
