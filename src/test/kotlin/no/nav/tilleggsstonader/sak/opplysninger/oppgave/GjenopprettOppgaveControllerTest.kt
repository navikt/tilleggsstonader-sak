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

        medBrukercontext(rolle = rolleConfig.utvikler) {
            kall.gjenopprettOppgave
                .gjenopprettResponse(behandling.id)
                .expectStatus()
                .isNoContent
        }

        assertThat(taskService.findAll().filter { it.type == GjenopprettOppgavePåBehandlingTask.TYPE }).hasSize(1)
    }

    @Test
    fun `gjenopprett feilregistrert på behandling, har ikke utviklerrolle, får 403`() {
        kall.gjenopprettOppgave
            .gjenopprettResponse(BehandlingId.random())
            .expectStatus()
            .isForbidden
    }
}
