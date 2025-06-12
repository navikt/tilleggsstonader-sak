package no.nav.tilleggsstonader.sak.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RevurderFraServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val nullstillBehandlingService = mockk<NullstillBehandlingService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>(relaxed = true)

    private val service =
        RevurderFraService(
            behandlingRepository = behandlingRepository,
            nullstillBehandlingService = nullstillBehandlingService,
            behandlingService = behandlingService,
        )

    private val oppdaterBehandlingSlot = slot<Behandling>()

    @BeforeEach
    fun setUp() {
        oppdaterBehandlingSlot.clear()
        every { behandlingRepository.update(capture(oppdaterBehandlingSlot)) } answers { firstArg() }
    }

    @Test
    fun `skal ikke kunne endre en behandling som er ferdigstilt`() {
        val behandling = mockBehandling(behandling(status = BehandlingStatus.FERDIGSTILT))

        val revurderFra = LocalDate.of(2024, 3, 1)
        assertThatThrownBy {
            service.oppdaterRevurderFra(behandling.id, revurderFra)
        }.hasMessageContaining("Kan ikke oppdatere revurder fra når behandlingen har status Ferdigstilt.")
        verify(exactly = 0) { nullstillBehandlingService.nullstillBehandling(any()) }
    }

    @Nested
    inner class Nullstilling {
        @Test
        fun `skal nullstille hvis revurderFra ikke er satt fra før`() {
            val behandling = mockBehandling(behandling(type = BehandlingType.REVURDERING))

            val revurderFra = LocalDate.of(2024, 3, 1)
            service.oppdaterRevurderFra(behandling.id, revurderFra)

            assertThat(oppdaterBehandlingSlot.captured.revurderFra).isEqualTo(revurderFra)
            verify(exactly = 1) { nullstillBehandlingService.nullstillBehandling(behandling.id) }
        }

        @Test
        fun `skal ikke nullstille revurderingsdato settes bak i tiden`() {
            val revurderFra = LocalDate.of(2024, 3, 1)
            val behandling =
                mockBehandling(behandling(type = BehandlingType.REVURDERING, revurderFra = revurderFra.plusDays(1)))

            service.oppdaterRevurderFra(behandling.id, revurderFra)

            assertThat(oppdaterBehandlingSlot.captured.revurderFra).isEqualTo(revurderFra)
            verify(exactly = 0) { nullstillBehandlingService.nullstillBehandling(any()) }
            verify(exactly = 1) { nullstillBehandlingService.slettVilkårperiodegrunnlag(any()) }
        }
    }

    private fun mockBehandling(behandling: Behandling): Behandling {
        every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        every { behandlingRepository.finnSaksbehandling(behandling.id) } returns saksbehandling(behandling = behandling)
        return behandling
    }
}
