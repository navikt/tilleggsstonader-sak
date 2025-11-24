package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.GjenopprettOppgavePåBehandlingTask
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GjenopprettOppgaveControllerTest : IntegrationTest() {
    @Test
    fun `gjenopprett feilregistrert på behandling, har utviklerrolle`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak()

        medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
            kall.gjenopprettOppgave.gjenopprett(behandling.id)
        }

        assertThat(taskService.findAll().filter { it.type == GjenopprettOppgavePåBehandlingTask.TYPE }).hasSize(1)
    }

    @Test
    fun `gjenopprett feilregistrert på behandling, har ikke utviklerrolle, får 403`() {
        kall.gjenopprettOppgave.apiRespons
            .gjenopprett(BehandlingId.random())
            .expectStatus()
            .isForbidden
    }
}
