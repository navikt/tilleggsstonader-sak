package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.GjenopprettOppgavePåBehandlingTask
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange

class GjenopprettOppgaveControllerTest : IntegrationTest() {
    @Autowired
    private lateinit var taskService: TaskService

    @Test
    fun `gjenopprett feilregistrert på behandling, har utviklerrolle`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak()

        val res =
            restTemplate.exchange<Unit>(
                localhost("/api/forvaltning/oppgave/gjenopprett/${behandling.id}"),
                HttpMethod.POST,
                HttpEntity(null, headers.apply { setBearerAuth(onBehalfOfToken(role = rolleConfig.utvikler)) }),
            )

        assertThat(res.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        assertThat(taskService.findAll().filter { it.type == GjenopprettOppgavePåBehandlingTask.TYPE }).hasSize(1)
    }

    @Test
    fun `gjenopprett feilregistrert på behandling, har ikke utviklerrolle, får 403`() {
        val res =
            catchThrowableOfType<HttpClientErrorException> {
                restTemplate.exchange<Unit>(
                    localhost("/api/forvaltning/oppgave/gjenopprett/${BehandlingId.random()}"),
                    HttpMethod.POST,
                    HttpEntity(null, headers.apply { setBearerAuth(onBehalfOfToken()) }), // får default rolle beslutter
                )
            }

        assertThat(res.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }
}
