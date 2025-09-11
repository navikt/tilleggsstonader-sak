package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.gjenopprettOppgaveKall
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.GjenopprettOppgavePåBehandlingTask
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GjenopprettOppgaveControllerTest : IntegrationTest() {
    @Test
    fun `gjenopprett feilregistrert på behandling, har utviklerrolle`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak()

        medBrukercontext(rolle = rolleConfig.utvikler) {
            gjenopprettOppgaveKall(behandling.id)
                .expectStatus()
                .isNoContent
        }

        assertThat(taskService.findAll().filter { it.type == GjenopprettOppgavePåBehandlingTask.TYPE }).hasSize(1)
    }

    @Test
    fun `gjenopprett feilregistrert på behandling, har ikke utviklerrolle, får 403`() {
        gjenopprettOppgaveKall(BehandlingId.random())
            .expectStatus()
            .isForbidden
    }
}
