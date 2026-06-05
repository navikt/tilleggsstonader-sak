package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class BrevmottakereKall(
    private val testklient: Testklient,
) {
    fun hent(behandlingId: BehandlingId) = apiRespons.hentBrevmottakere(behandlingId).expectOkWithBody<BrevmottakereDto>()

    val apiRespons = BrevmottakereApi()

    inner class BrevmottakereApi {
        fun hentBrevmottakere(behandlingId: BehandlingId) = testklient.get("/api/brevmottakere/$behandlingId")
    }
}
