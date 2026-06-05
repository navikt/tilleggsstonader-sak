package no.nav.tilleggsstonader.sak.googlemaps.nvdbApi

import org.springframework.stereotype.Service

@Service
class BomstasjonService(
    private val nvdbBomstasjonClient: NvdbBomstasjonClient,
) {
    fun hentAlleBomstasjoner(): List<NvdbBomstasjon> = nvdbBomstasjonClient.hentAlleBomstasjoner()

    fun harBomstasjonPåRute(encodedPolyline: String): Boolean {
        val punkter = encodedPolyline.decodePolyline()
        val bomstasjoner = hentAlleBomstasjoner()

        val terskelMeter = 75.0

        val treffStasjoner =
            bomstasjoner
                .filter { stasjon ->
                    punkter.any { punkt ->
                        finnBomstasjonPåRute(punkt.lat, punkt.lng, stasjon.lat, stasjon.lng) < terskelMeter
                    }
                }.distinctBy { it.navn }

        if (treffStasjoner.isNotEmpty()) {
            println("${treffStasjoner.size} bomstasjon(er) funnet på ruten:")
            treffStasjoner.forEach { stasjon ->
                println("  - id=${stasjon.id}, navn=${stasjon.navn}, lat=${stasjon.lat}, lng=${stasjon.lng}")
            }
        } else {
            println("Ingen bomstasjon funnet på ruten")
        }

        return treffStasjoner.isNotEmpty()
    }
}
