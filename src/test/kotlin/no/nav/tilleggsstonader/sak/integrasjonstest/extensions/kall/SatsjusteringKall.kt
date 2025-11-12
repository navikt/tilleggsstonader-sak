package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

// Må kjøres med utvikler-rolle
class SatsjusteringKall(
    private val testklient: Testklient,
) {
    fun satsjustering(stønadstype: Stønadstype): List<BehandlingId> = apiRespons.satsjustering(stønadstype).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = SatsjusteringApi()

    inner class SatsjusteringApi {
        fun satsjustering(stønadstype: Stønadstype) = testklient.post("/api/forvaltning/satsjustering/$stønadstype", Unit)
    }
}
