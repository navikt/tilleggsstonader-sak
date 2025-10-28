package no.nav.tilleggsstonader.sak.behandling

import io.mockk.every
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OpprettBehandlingServiceIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var opprettBehandlingService: OpprettBehandlingService

    private val behandlingÅrsak = BehandlingÅrsak.SØKNAD

    @Test
    internal fun `skal feile hvis krav mottatt er frem i tid`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandlingRequest(
                        fagsakId = fagsak.id,
                        stegType = StegType.VILKÅR,
                        behandlingsårsak = BehandlingÅrsak.PAPIRSØKNAD,
                        kravMottatt = LocalDate.now().plusDays(1),
                    ),
                )
            }.withMessage("Kan ikke sette krav mottattdato frem i tid")
    }

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
        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandlingRequest(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                    ),
                )
            }.withMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
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
        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandlingRequest(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                    ),
                )
            }.withMessage("Det finnes en behandling på fagsaken som hverken er ferdigstilt eller satt på vent")
    }

    @Nested
    inner class BehandlingPåVent {
        // TODO: Slett når snike i køen er implementert
        @Test
        internal fun `opprettBehandling av førstegangsbehandling er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns false
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatExceptionOfType(ApiFeil::class.java)
                .isThrownBy {
                    opprettBehandlingService.opprettBehandling(
                        OpprettBehandlingRequest(
                            fagsak.id,
                            behandlingsårsak = behandlingÅrsak,
                        ),
                    )
                }.withMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        internal fun `opprettBehandling av førstegangsbehandling er mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns true
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))

            // Sjekker at denne ikke kaster feil
            opprettBehandlingService.opprettBehandling(
                OpprettBehandlingRequest(
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                ),
            )
        }

        // TODO: Slett når snike i køen er implementert
        @Test
        internal fun `opprettBehandling av revurdering er ikke mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns false
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            assertThatExceptionOfType(ApiFeil::class.java)
                .isThrownBy {
                    opprettBehandlingService.opprettBehandling(
                        OpprettBehandlingRequest(
                            fagsak.id,
                            behandlingsårsak = behandlingÅrsak,
                        ),
                    )
                }.withMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        internal fun `opprettBehandling av revurdering er mulig hvis det finnes en førstegangsbehandling på vent`() {
            every { unleashService.isEnabled(Toggle.KAN_HA_FLERE_BEHANDLINGER_PÅ_SAMME_FAGSAK) } returns true
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            testoppsettService.lagre(behandling(fagsak, BehandlingStatus.SATT_PÅ_VENT))
            // Sjekker at denne ikke kaster feil
            opprettBehandlingService.opprettBehandling(
                OpprettBehandlingRequest(
                    fagsak.id,
                    behandlingsårsak = behandlingÅrsak,
                ),
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
        assertThatExceptionOfType(ApiFeil::class.java)
            .isThrownBy {
                opprettBehandlingService.opprettBehandling(
                    OpprettBehandlingRequest(
                        fagsak.id,
                        behandlingsårsak = behandlingÅrsak,
                    ),
                )
            }.withMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
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
        opprettBehandlingService.opprettBehandling(
            OpprettBehandlingRequest(
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            ),
        )
    }
}
