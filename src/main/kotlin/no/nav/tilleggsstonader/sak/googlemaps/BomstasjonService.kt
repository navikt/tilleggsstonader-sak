package no.nav.tilleggsstonader.sak.googlemaps

import jakarta.annotation.PostConstruct
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.googlemaps.nvdbApi.NvdbBomstasjon
import no.nav.tilleggsstonader.sak.googlemaps.nvdbApi.NvdbBomstasjonClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class BomstasjonService(
    private val nvdbBomstasjonClient: NvdbBomstasjonClient,
) {
    @Volatile
    private var bomstasjoner: List<NvdbBomstasjon> = emptyList()

    @PostConstruct
    @Scheduled(cron = "0 0 3 * * *")
    fun oppdaterBomstasjoner() {
        runCatching {
            bomstasjoner = nvdbBomstasjonClient.hentAlleBomstasjoner()
            logger.info("Lastet {} bomstasjoner fra NVDB", bomstasjoner.size)
        }.onFailure { e ->
            logger.error("Klarte ikke laste bomstasjoner fra NVDB", e)
        }
    }

    fun harBomvei(encodedPolyline: String): Boolean {
        val punkter = encodedPolyline.decodePolyline()
        return bomstasjoner.any { stasjon ->
            punkter.any { punkt ->
                haversineDistanseMeter(punkt.lat, punkt.lng, stasjon.lat, stasjon.lng) < TERSKEL_METER
            }
        }
    }

    companion object {
        private const val TERSKEL_METER = 75.0
    }
}
