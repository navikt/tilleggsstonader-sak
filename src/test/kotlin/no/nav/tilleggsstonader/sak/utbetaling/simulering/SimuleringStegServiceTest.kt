package no.nav.tilleggsstonader.sak.utbetaling.simulering

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class SimuleringStegServiceTest {

    val stegService = mockk<StegService>()
    val simuleringService = mockk<SimuleringService>()
    val tilgangService = mockk<TilgangService>()

    val simuleringStegService = SimuleringStegService(
        stegService = stegService,
        simuleringService = simuleringService,
        tilgangService = tilgangService,
    )

    val fagsak = fagsak()

    @BeforeEach
    fun setUp() {
        every { tilgangService.harTilgangTilRolle(any()) } returns true
    }

    @Test
    internal fun `skal hente lagret simulering hvis behandlingen ikke er redigerbar`() {
        val saksbehandling =
            saksbehandling(
                fagsak = fagsak,
                type = BehandlingType.REVURDERING,
                status = BehandlingStatus.FATTER_VEDTAK,
            )

        every { simuleringService.hentLagretSimulering(any()) } returns Simuleringsresultat(behandlingId = BehandlingId(UUID.randomUUID()), data = null)
        val simuleringsresultatDto = simuleringStegService.hentEllerOpprettSimuleringsresultat(saksbehandling)

        verify { stegService wasNot Called }

        assertThat(simuleringsresultatDto).isNotNull
    }
}
