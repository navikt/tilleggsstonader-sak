package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InngangsvilkårStegTest {
    val behandlingService = mockk<BehandlingService>()

    val steg =
        InngangsvilkårSteg(
            behandlingService = behandlingService,
        )

    @BeforeEach
    fun setUp() {
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns behandling()
        justRun { behandlingService.markerBehandlingSomPåbegynt(any(), any()) }
    }

    @Nested
    inner class TilsynBarn {
        @Test
        fun `Neste steg - skal innom vilkår`() {
            val behandling = saksbehandling()
            val nesteSteg = steg.utførOgReturnerNesteSteg(behandling, null)

            assertThat(nesteSteg).isEqualTo(StegType.VILKÅR)
            verify(exactly = 1) {
                behandlingService.markerBehandlingSomPåbegynt(any(), any())
            }
        }

        @Nested
        inner class Læremidler {
            @Test
            fun `Neste steg - har ikke noen vilkår og kan hoppe direkte til beregne ytelse`() {
                val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER))
                val nesteSteg = steg.utførOgReturnerNesteSteg(behandling, null)

                assertThat(nesteSteg).isEqualTo(StegType.BEREGNE_YTELSE)
                verify(exactly = 1) {
                    behandlingService.markerBehandlingSomPåbegynt(any(), any())
                }
            }
        }
    }
}
