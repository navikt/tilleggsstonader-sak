package no.nav.tilleggsstonader.sak.utbetaling.simulering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtaksresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SimuleringStegTest {

    val simuleringService = mockk<SimuleringService>()
    val vedtaksresultatService = mockk<VedtaksresultatService>()

    val simuleringSteg = SimuleringSteg(simuleringService, vedtaksresultatService)

    @BeforeEach
    fun setUp() {
        every { simuleringService.hentOgLagreSimuleringsresultat(any()) } returns mockk()
    }

    private fun mockVedtakMedType(type: TypeVedtak) =
        every { vedtaksresultatService.hentVedtaksresultat(any()) } returns type

    @Nested
    inner class Revurdering {

        @Test
        fun `skal utføre simulering for innvilget revurdering`() {
            val saksbehandling = saksbehandling(
                type = BehandlingType.REVURDERING,
            )
            mockVedtakMedType(TypeVedtak.INNVILGELSE)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }

        @Test
        fun `skal ikke utføre simulering for avslått revurdering`() {
            val saksbehandling = saksbehandling(
                type = BehandlingType.REVURDERING,
            )
            mockVedtakMedType(TypeVedtak.AVSLAG)

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
            )
            mockVedtakMedType(TypeVedtak.INNVILGELSE)

            simuleringSteg.utførSteg(saksbehandling, null)

            verify(exactly = 0) { simuleringService.hentOgLagreSimuleringsresultat(saksbehandling) }
        }
    }
}
