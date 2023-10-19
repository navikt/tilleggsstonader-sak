package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StegService(
    private val behandlingService: BehandlingService,
) {

    fun <T> håndterSteg(
        behandlingId: UUID,
        behandlingSteg: BehandlingSteg<T>,
        data: T,
    ) {
        håndterSteg(
            behandlingService.hentSaksbehandling(behandlingId),
            behandlingSteg,
            data,
        )
    }
    fun <T> håndterSteg(
        saksbehandling: Saksbehandling,
        behandlingSteg: BehandlingSteg<T>,
        data: T,
    ) {
        // TODO implementer det andre som skal skje her
        behandlingSteg.utførSteg(saksbehandling, data)
    }
}
