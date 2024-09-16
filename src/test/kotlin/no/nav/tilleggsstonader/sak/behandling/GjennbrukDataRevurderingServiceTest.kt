package no.nav.tilleggsstonader.sak.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GjennbrukDataRevurderingServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val barnService = mockk<BarnService>()
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val stønadsperiodeService = mockk<StønadsperiodeService>()
    val vilkårService = mockk<VilkårService>()

    val service = GjennbrukDataRevurderingService(
        behandlingService = behandlingService,
        barnService = barnService,
        vilkårperiodeService = vilkårperiodeService,
        stønadsperiodeService = stønadsperiodeService,
        vilkårService = vilkårService,
    )

    val fagsakId = FagsakId.randomUUID()
    val iverksattFerdigstiltBehandling = behandling()
    val henlagtBehandling = behandling(
        status = BehandlingStatus.FERDIGSTILT,
        resultat = BehandlingResultat.HENLAGT,
    )
    val avslåttBehandling = behandling(
        status = BehandlingStatus.FERDIGSTILT,
        resultat = BehandlingResultat.AVSLÅTT,
    )
    val opphørtBehandling = behandling(
        status = BehandlingStatus.FERDIGSTILT,
        resultat = BehandlingResultat.OPPHØRT,
    )

    @BeforeEach
    fun setUp() {
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns null
        every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns emptyList()
    }

    @Nested
    inner class FinnBehandlingIdForGjenbrukFraBehandling {

        @Test
        fun `skal bruke forrige behandlingId hvis den finnes på behandling som man sender inn`() {
            val behandling = behandling(forrigeBehandlingId = BehandlingId.random())

            assertThat(service.finnBehandlingIdForGjenbruk(behandling)).isEqualTo(behandling.forrigeBehandlingId)

            verify(exactly = 0) { behandlingService.hentBehandlinger(any<FagsakId>()) }
        }

        @Test
        fun `skal bruke siste avslåtte behandling hvis forrigeBehandlingId på behandling er null`() {
            val behandling = behandling()
            every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns listOf(avslåttBehandling)

            assertThat(service.finnBehandlingIdForGjenbruk(behandling)).isEqualTo(avslåttBehandling.id)

            verify(exactly = 1) { behandlingService.hentBehandlinger(any<FagsakId>()) }
        }
    }

    @Nested
    inner class FinnBehandlingIdForGjenbrukFraFagsakId {

        @Test
        fun `skal finne siste iverksatte behandling hvis den finnes`() {
            every { behandlingService.finnSisteIverksatteBehandling(any()) } returns iverksattFerdigstiltBehandling

            assertThat(service.finnBehandlingIdForGjenbruk(fagsakId)).isEqualTo(iverksattFerdigstiltBehandling.id)
        }

        @Test
        fun `skal bruke siste behandling som er avslått hvis det finnes henlagte behandlinger før den avslåtte`() {
            every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns listOf(
                henlagtBehandling,
                avslåttBehandling,
                henlagtBehandling,
            )

            assertThat(service.finnBehandlingIdForGjenbruk(fagsakId)).isEqualTo(avslåttBehandling.id)
        }

        @Test
        fun `skal bruke siste behandling som er opphørt hvis det finnes henlagte behandlinger før den opphørte`() {
            every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns listOf(
                henlagtBehandling,
                opphørtBehandling,
                henlagtBehandling,
            )

            assertThat(service.finnBehandlingIdForGjenbruk(fagsakId)).isEqualTo(opphørtBehandling.id)
        }

        @Test
        fun `skal returnere null hvis det kun finnes henlagte behandlinger`() {
            every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns listOf(henlagtBehandling)

            assertThat(service.finnBehandlingIdForGjenbruk(fagsakId)).isNull()
        }
    }
}
