package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.behandling.vent.KanTaAvVentDto
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVentDto
import no.nav.tilleggsstonader.sak.behandling.vent.StatusPåVentDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient

class SettPåVentKall(
    private val testklient: Testklient,
) {
    fun settPaVent(
        behandlingId: BehandlingId,
        settPåVentDto: SettPåVentDto,
    ): StatusPåVentDto = apiRespons.settPaVent(behandlingId, settPåVentDto).expectOkWithBody()

    fun kanTaAvVent(behandlingId: BehandlingId): KanTaAvVentDto = apiRespons.kanTaAvVent(behandlingId).expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = SettPåVentApi()

    inner class SettPåVentApi {
        fun settPaVent(
            behandlingId: BehandlingId,
            settPåVentDto: SettPåVentDto,
        ) = testklient.post("/api/sett-pa-vent/$behandlingId", settPåVentDto)

        fun kanTaAvVent(behandlingId: BehandlingId) = testklient.get("/api/sett-pa-vent/$behandlingId/kan-ta-av-vent")
    }
}
