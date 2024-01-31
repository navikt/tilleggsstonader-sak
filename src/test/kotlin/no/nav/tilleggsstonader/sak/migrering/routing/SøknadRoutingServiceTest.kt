package no.nav.tilleggsstonader.sak.migrering.routing

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.arena.VedtakStatus
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.util.EnvUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadRoutingServiceTest {

    val søknadRoutingRepository = mockk<SøknadRoutingRepository>()
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val arenaClient = mockk<ArenaClient>()
    val personService = mockk<PersonService>()

    private val service = SøknadRoutingService(
        søknadRoutingRepository,
        fagsakService,
        behandlingService,
        arenaClient,
        personService,
    )

    private val ident = "1"
    private val stønadstype = Stønadstype.BARNETILSYN

    private val søknadRouting = SøknadRouting(ident = ident, type = stønadstype, detaljer = JsonWrapper(""))
    private val request = IdentStønadstype(ident, stønadstype)

    @BeforeEach
    fun setUp() {
        mockkObject(EnvUtil)
        every { EnvUtil.erIDev() } returns true

        every { søknadRoutingRepository.findByIdentAndType(any(), any()) } returns null
        every { søknadRoutingRepository.countByType(any()) } returns 10
        every { søknadRoutingRepository.insert(any()) } answers { firstArg() }
        every { fagsakService.finnFagsak(any(), any()) } returns null
        every { behandlingService.hentBehandlinger(any<UUID>()) } returns emptyList()
        every { arenaClient.hentStatus(any()) } returns arenaStatusKanIkkeRoutes()
        every { personService.hentPersonIdenter(any()) } answers { PdlIdenter(listOf(PdlIdent(firstArg(), false))) }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(EnvUtil)
    }

    @Test
    fun `skal returnere true hvis det finnes innslag i databasen`() {
        every { søknadRoutingRepository.findByIdentAndType(ident, stønadstype) } returns søknadRouting

        assertThat(skalBehandlesINyLøsning()).isTrue()

        verify {
            søknadRoutingRepository.findByIdentAndType(ident, stønadstype)
            søknadRoutingRepository.insert(any()) wasNot called

            fagsakService wasNot called
            behandlingService wasNot called
            arenaClient wasNot called
        }
    }

    @Test
    fun `skal ikke sjekke behandling eller arena hvis antall er nått`() {
        every { søknadRoutingRepository.countByType(any()) } returns 10000

        assertThat(skalBehandlesINyLøsning()).isFalse()

        verify {
            søknadRoutingRepository.insert(any()) wasNot called

            fagsakService wasNot called
            behandlingService wasNot called
            arenaClient wasNot called
        }
    }

    @Nested
    inner class HarBehandling {

        @Test
        fun `skal sjekke om det finnes behandling hvis det ikke innslag i databasen`() {
            assertThat(skalBehandlesINyLøsning()).isFalse()

            verify {
                fagsakService.finnFagsak(any(), any())
                søknadRoutingRepository.insert(any()) wasNot called
                behandlingService wasNot called
            }
        }

        @Test
        fun `skal oppdatere reopository hvis det finnes en behandling`() {
            val fagsak = fagsak()
            every { fagsakService.finnFagsak(any(), any()) } returns fagsak
            every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling())

            assertThat(skalBehandlesINyLøsning()).isTrue()

            verify {
                fagsakService.finnFagsak(any(), any())
                behandlingService.hentBehandlinger(any<UUID>())
                søknadRoutingRepository.insert(any())
                arenaClient wasNot called
            }
        }
    }

    @Nested
    inner class ArenaStatus {

        @Test
        fun `skal sjekke arenstatus hvis det ikke finnes innslag i databasen eller behandlinger`() {
            assertThat(skalBehandlesINyLøsning()).isFalse()

            verify {
                arenaClient.hentStatus(any())
                søknadRoutingRepository.insert(any()) wasNot called
            }
        }

        @Test
        fun `skal oppdatere databasen hvis statusen i arena tilsier sånn`() {
            every { arenaClient.hentStatus(any()) } returns arenaStatusKanRoutes()

            assertThat(skalBehandlesINyLøsning()).isTrue()

            verify {
                fagsakService.finnFagsak(any(), any())
                arenaClient.hentStatus(any())
                søknadRoutingRepository.insert(any())
                behandlingService wasNot called
            }
        }
    }

    private fun arenaStatusKanIkkeRoutes() = ArenaStatusDto(
        SakStatus(harAktivSakUtenVedtak = true),
        VedtakStatus(harVedtak = true, harAktivtVedtak = true),
    )

    private fun arenaStatusKanRoutes() = ArenaStatusDto(
        SakStatus(harAktivSakUtenVedtak = false),
        VedtakStatus(harVedtak = false, harAktivtVedtak = false),
    )

    private fun skalBehandlesINyLøsning() = service.sjekkRoutingForPerson(request).skalBehandlesINyLøsning
}
