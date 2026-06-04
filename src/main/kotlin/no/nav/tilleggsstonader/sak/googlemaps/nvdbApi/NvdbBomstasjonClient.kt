package no.nav.tilleggsstonader.sak.googlemaps.nvdbApi

import no.nav.tilleggsstonader.libs.log.logger
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
            .baseUrl("https://nvdbapiles.test.atlas.vegvesen.no/")
            .defaultHeader("X-Client", "tilleggsstonader-sak")
            .build()

    fun hentAlleBomstasjoner(): List<NvdbBomstasjon> {
        val alle = mutableListOf<NvdbBomstasjon>()
        var startParam: String? = null
        do {
            val response = hentSide(startParam)
            alle.addAll(response.objekter.mapNotNull { it.tilDomene() })
            startParam = response.metadata.neste?.start
        } while (startParam != null)
        logger.info("Hentet totalt {} bomstasjoner fra NVDB", alle.size)
        return alle
    }

    private fun hentSide(start: String?): NvdbBomstasjonResponse =
        restClient
            .get()
            .uri { builder ->
                builder
                    .path("/vegobjekter/45")
                    .queryParam("srid", "4326")
                    .queryParam("inkluder", "metadata,egenskaper,lokasjon")
                    .queryParam("alle_versjoner", "false")
                    .apply { if (start != null) queryParam("start", start) }
                    .build()
            }.retrieve()
            .body<NvdbBomstasjonResponse>()!!
}
