package no.nav.tilleggsstonader.sak.utbetaling.simulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SimuleringStegTest {

    val simuleringService = mockk<SimuleringService>()

    val simuleringSteg = SimuleringSteg(simuleringService)

    @BeforeEach
    fun setUp() {
        every { simuleringService.hentOgLagreSimuleringsresultat(any()) } returns mockk()
    }

    @Nested
    inner class Revurdering {

        @Test
        fun `skal utføre simulering for innvilget revurdering`() {
            val saksbehandling = saksbehandling(
                type = BehandlingType.REVURDERING,
                resultat = BehandlingResultat.INNVILGET,
            )

            simuleringSteg.utførSteg(saksbehandling, null)

            verify { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal utføre simulering for opphørt revurdering`() {
            val saksbehandling = saksbehandling(
                type = BehandlingType.REVURDERING,
                resultat = BehandlingResultat.OPPHØRT,
            )

            simuleringSteg.utførSteg(saksbehandling, null)

            verify { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering for avslått revurdering`() {
            val saksbehandling = saksbehandling(
                type = BehandlingType.REVURDERING,
                resultat = BehandlingResultat.AVSLÅTT,
            )

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }
    }

    @Nested
    inner class Førstegangsbehandling {

        @Test
        fun `skal ikke utføre simulering for førstegangsbehandling`() {
            val saksbehandling = saksbehandling(
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                resultat = BehandlingResultat.INNVILGET,
            )

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }
    }
}
