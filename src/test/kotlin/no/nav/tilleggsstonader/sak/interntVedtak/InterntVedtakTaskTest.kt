package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class InterntVedtakTaskTest {
    private val interntVedtakService = mockk<InterntVedtakService>()
    private val htmlifyClient = mockk<HtmlifyClient>()
    private val dokumentClient = mockk<FamilieDokumentClient>()
    private val journalpostService = mockk<JournalpostService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    private val task =
        InterntVedtakTask(
            interntVedtakService = interntVedtakService,
            htmlifyClient = htmlifyClient,
            dokumentClient = dokumentClient,
            journalpostService = journalpostService,
            arbeidsfordelingService = arbeidsfordelingService,
        )

    @Test
    fun `htmlify-feil skal boble opp uten å kalle dokument- eller journalføringstjenester`() {
        val behandlingId = BehandlingId.random()
        val interntVedtak = mockk<InterntVedtak>()
        val feil = HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Htmlify-feil")
        every { interntVedtakService.lagInterntVedtak(behandlingId) } returns interntVedtak
        every { htmlifyClient.generateHtml(interntVedtak) } throws feil

        assertThatThrownBy { task.doTask(InterntVedtakTask.lagTask(behandlingId)) }
            .isSameAs(feil)

        verify { listOf(dokumentClient, journalpostService, arbeidsfordelingService) wasNot Called }
    }
}
