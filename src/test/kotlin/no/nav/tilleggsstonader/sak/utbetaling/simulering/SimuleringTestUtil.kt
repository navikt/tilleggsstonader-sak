package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringJson
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat

object SimuleringTestUtil {

    fun simuleringsresultat(
        behandlingId: BehandlingId = BehandlingId.random(),
        data: SimuleringJson? = null,
    ) = Simuleringsresultat(behandlingId, data = data)
}
