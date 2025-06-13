package no.nav.tilleggsstonader.sak.behandling

import io.mockk.every
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingServiceIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var behandlingService: BehandlingService
    private val behandlingÅrsak = BehandlingÅrsak.SØKNAD

    // TODO: Slett når snike i køen er implementert
    @Test
    internal fun `skal ikke være mulig å opprette en revurdering om forrige behandling ikke er ferdigstilt`() {
        every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns false
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(
            behandling(
                fagsak = fagsak,
                status = BehandlingStatus.UTREDES,
            ),
        )
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `skal ikke være mulig å opprette en revurdering om forrige behandling ikke er ferdigstilt eller på vent`() {
        every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns true
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(
            behandling(
                fagsak = fagsak,
                status = BehandlingStatus.UTREDES,
            ),
        )
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }.hasMessage("Det finnes en behandling på fagsaken som hverken er ferdigstilt eller satt på vent")
    }

    @Test
    internal fun `hentBehandlinger - skal kaste feil hvis behandling ikke finnes`() {
        assertThatThrownBy { behandlingService.hentBehandlinger(setOf(BehandlingId.random())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Finner ikke Behandling for")
    }

    @Test
    internal fun `hentBehandlinger - skal returnere behandlinger`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = testoppsettService.lagre(behandling(fagsak))

        assertThat(behandlingService.hentBehandlinger(setOf(behandling.id, behandling2.id))).hasSize(2)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis kun avslått`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling =
            testoppsettService.lagre(
                behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
            )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(behandling.id)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val avslag =
            testoppsettService.lagre(
                behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
            )
        testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(avslag.id)
    }

    @Test
    internal fun `skal plukke ut førstegangsbehandling hvis det finnes førstegangsbehandling, avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val førstegang =
            testoppsettService.lagre(
                behandling(fagsak, resultat = BehandlingResultat.INNVILGET, status = BehandlingStatus.FERDIGSTILT),
            )
        testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
        )
        testoppsettService.lagre(
            behandling(fagsak, resultat = BehandlingResultat.HENLAGT, status = BehandlingStatus.FERDIGSTILT),
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(førstegang.id)
    }

    @Nested
    inner class BehandlingPåVent {
        // TODO: Slett når snike i køen er implementert
        @Test
        internal fun `opprettBehandling av førstegangsbehandling er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns false
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatThrownBy {
                behandlingService.opprettBehandling(
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                )
            }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        internal fun `opprettBehandling av førstegangsbehandling er mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns true
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))

            // Sjekker at denne ikke kaster feil
            behandlingService.opprettBehandling(
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }

        // TODO: Slett når snike i køen er implementert
        @Test
        internal fun `opprettBehandling av revurdering er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns false
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatThrownBy {
                behandlingService.opprettBehandling(
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                )
            }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        internal fun `opprettBehandling av revurdering er mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns true
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            // Sjekker at denne ikke kaster feil
            behandlingService.opprettBehandling(
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }
    }

    // TODO: Slett når snike i køen er implementert
    @Test
    internal fun `opprettBehandling er ikke mulig hvis det finnes en revurdering på vent`() {
        every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns false
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(behandling(fagsak, BehandlingStatus.FERDIGSTILT))
        testoppsettService.lagre(
            behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT, type = BehandlingType.REVURDERING),
        )
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `opprettBehandling er mulig hvis det finnes en revurdering på vent`() {
        every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns true
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        testoppsettService.lagre(behandling(fagsak, BehandlingStatus.FERDIGSTILT))
        testoppsettService.lagre(
            behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT, type = BehandlingType.REVURDERING),
        )
        // Sjekker at denne ikke kaster feil
        behandlingService.opprettBehandling(
            fagsak.id,
            behandlingsårsak = behandlingÅrsak,
        )
    }
}
