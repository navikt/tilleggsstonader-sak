package no.nav.tilleggsstonader.sak.ekstern.stønad

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.VedtakStatus
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.ArenaClientConfig.Companion.resetMock
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EksternVedtakServiceTest : IntegrationTest() {

    @Autowired
    lateinit var arenaClient: ArenaClient

    @Autowired
    lateinit var service: EksternVedtakService

    val fagsak = fagsak()
    val request = IdentRequest(fagsak.hentAktivIdent())

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        mockArena(harInnvilgetVedtak = false)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        resetMock(arenaClient)
    }

    @Test
    fun `har vedtak hvis personen har en iverksatt behandling`() {
        val behandling = behandling(fagsak, status = BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)

        val result = service.hentVedtaksinformasjonTilsynBarn(request)

        assertThat(result.harInnvilgetVedtak).isTrue

        verify(exactly = 0) { arenaClient.hentStatus(any()) }
    }

    @Test
    fun `skal sjekke arena hvis personen ikke har en iverksatt behandling i ny løsning`() {
        mockArena(harInnvilgetVedtak = true)

        val result = service.hentVedtaksinformasjonTilsynBarn(request)

        assertThat(result.harInnvilgetVedtak).isTrue

        verify(exactly = 1) { arenaClient.hentStatus(any()) }
    }

    @Test
    fun `har ikke vedtak hvis personen ikke har iverksatt behandling eller vedtak i arena`() {
        val result = service.hentVedtaksinformasjonTilsynBarn(request)

        assertThat(result.harInnvilgetVedtak).isFalse

        verify(exactly = 1) { arenaClient.hentStatus(any()) }
    }

    private fun mockArena(harInnvilgetVedtak: Boolean) {
        every { arenaClient.hentStatus(any()) } returns ArenaStatusDto(
            sak = mockk(),
            vedtak = VedtakStatus(
                harVedtak = false,
                harInnvilgetVedtak = harInnvilgetVedtak,
                harAktivtVedtak = false,
                harVedtakUtenUtfall = false,
                vedtakTom = null,
            ),
        )
    }
}
