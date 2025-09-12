package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class InngangsvilkårStegTest {
    val behandlingService = mockk<BehandlingService>()
    val vilkårperiodeService = mockk<VilkårperiodeService>()

    val steg =
        InngangsvilkårSteg(
            behandlingService = behandlingService,
            vilkårperiodeService = vilkårperiodeService,
        )

    @BeforeEach
    fun setUp() {
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns behandling()
        justRun { behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(any(), any(), any()) }
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(emptyList(), emptyList())
    }

    @Test
    fun `Neste steg - skal innom vilkår`() {
        val behandling = saksbehandling()
        val nesteSteg = steg.utførOgReturnerNesteSteg(behandling, null)

        assertThat(nesteSteg).isEqualTo(StegType.VILKÅR)
        verify(exactly = 1) {
            behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(any(), any(), any())
        }
    }

    @Test
    fun `Neste steg - har ikke noen vilkår og kan hoppe direkte til beregne ytelse`() {
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER))
        val nesteSteg = steg.utførOgReturnerNesteSteg(behandling, null)

        assertThat(nesteSteg).isEqualTo(StegType.BEREGNE_YTELSE)
        verify(exactly = 1) {
            behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(any(), any(), any())
        }
    }

    @Nested
    inner class Validering {
        @Test
        fun `skal kaste feil dersom det ikke er lagt inn noen vilkårperioder`() {
            assertThatThrownBy {
                steg.validerSteg(saksbehandling())
            }.hasMessageContaining("Du må registrere minst én målgruppe og én aktivitet for å kunne gå videre.")
        }

        @Test
        fun `skal ikke kaste feil dersom det er registrert minst én aktivitet og én målgruppe`() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns
                Vilkårperioder(
                    listOf(målgruppe()),
                    listOf(aktivitet()),
                )
            assertDoesNotThrow {
                steg.validerSteg(saksbehandling())
            }
        }
    }
}
