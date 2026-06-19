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
        val terskelMeter = 30.0

        return bomstasjoner
            .filter { stasjon ->
                beregnKortestAvstandFraPunktTilRute(stasjon.lat, stasjon.lng, punkter) < terskelMeter
            }.distinctBy { it.navn }
            .isNotEmpty()
    }
}
