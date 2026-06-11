package no.nav.tilleggsstonader.sak.nvdbApi

import org.springframework.stereotype.Service

@Service
class BomstasjonService(
    private val nvdbBomstasjonClient: NvdbBomstasjonClient,
) {
    fun hentAlleBomstasjoner(): List<NvdbBomstasjon> = nvdbBomstasjonClient.hentAlleBomstasjoner()

    fun harBomstasjonPåRute(encodedPolyline: String): Boolean {
        val punkter = encodedPolyline.decodePolyline()
        val bomstasjoner = hentAlleBomstasjoner()
        val terskelMeter = 50.0

        val treffStasjoner =
            bomstasjoner
                .filter { stasjon ->
                    punkter.any { punkt ->
                        finnBomstasjonPåRute(punkt.lat, punkt.lng, stasjon.lat, stasjon.lng) < terskelMeter
                    }
                }.distinctBy { it.navn }

        return treffStasjoner.isNotEmpty()
    }
}
