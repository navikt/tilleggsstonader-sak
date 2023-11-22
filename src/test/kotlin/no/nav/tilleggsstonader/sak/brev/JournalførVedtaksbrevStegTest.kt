package no.nav.tilleggsstonader.sak.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksbrev
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.HttpClientErrorException
import java.net.http.HttpTimeoutException

class JournalførVedtaksbrevStegTest {

    val brevService = mockk<BrevService>()
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    val journalpostService = mockk<JournalpostService>()
    val brevmottakerRepository = mockk<BrevmottakerRepository>()

    private val journalførVedtaksbrevSteg = JournalførVedtaksbrevSteg(
        brevService,
        arbeidsfordelingService,
        journalpostService,
        brevmottakerRepository,
    )

    val saksbehandling = saksbehandling()

    @BeforeEach
    fun setUp() {
        every { brevService.hentBesluttetBrev(saksbehandling.id) } returns vedtaksbrev(behandlingId = saksbehandling.id)
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns ArbeidsfordelingService.ENHET_NASJONAL_NAY
        every { brevmottakerRepository.insert(any()) } returns mockk()
    }

    @Test
    internal fun `steg skal feile dersom kall mot dokarkiv feiler, og feilen ikke er 409 Conflict`() {
        val feil = HttpTimeoutException("")
        every { journalpostService.opprettJournalpost(any()) } throws feil

        assertThatThrownBy {
            journalførVedtaksbrevSteg.utførSteg(saksbehandling, null)
        }.hasCause(feil.cause)
    }

    @Test
    internal fun `steg skal ikke feile dersom kall mot dokarkiv feiler, og feilen er 409 Conflict`() {
        every { journalpostService.opprettJournalpost(any()) } throws HttpClientErrorException(HttpStatusCode.valueOf(409))

        journalførVedtaksbrevSteg.utførSteg(saksbehandling, null)
    }
}
