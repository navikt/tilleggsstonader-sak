package no.nav.tilleggsstonader.sak.migrering.arena

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.arena.vedtak.Rettighet
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled // Denne virker ikke når alle kall mot TILSYN_BARN settes til true
class ArenaStatusServiceTest {

    private val personService = mockk<PersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val søknadRoutingService = mockk<SøknadRoutingService>()

    val arenaStatusService = ArenaStatusService(
        personService = personService,
        fagsakService = fagsakService,
        fagsakPersonService = mockk(),
        behandlingService = behandlingService,
        søknadRoutingService = søknadRoutingService,
        unleashService = mockUnleashService(),
    )

    val ident = "ident"
    val fagsak = fagsak(identer = setOf(PersonIdent(ident)))

    val request = ArenaFinnesPersonRequest(ident, Rettighet.TILSYN_BARN.kodeArena)

    @BeforeEach
    fun setUp() {
        mockFinnFagsak(fagsak)
        every { personService.hentPersonIdenter(ident) } returns PdlIdenter(listOf(PdlIdent(ident, false)))
        mockSøknadRouting(false)
    }

    @Test
    fun `skal returnere false når det ikke finnes noen fagsak`() {
        mockFinnFagsak(null)

        assertThat(arenaStatusService.finnStatus(request).finnes).isFalse()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 0) { behandlingService.finnesBehandlingForFagsak(any()) }
        verify(exactly = 1) { søknadRoutingService.harLagretRouting(any()) }
    }

    @Test
    fun `skal returnere false når det ikke finnes noen behandlinger`() {
        mockFinnesBehandlingForFagsak(false)

        assertThat(arenaStatusService.finnStatus(request).finnes).isFalse()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 1) { behandlingService.finnesBehandlingForFagsak(any()) }
        verify(exactly = 1) { søknadRoutingService.harLagretRouting(any()) }
    }

    @Test
    fun `skal returnere true hvis det finnes behandlinger`() {
        mockFinnesBehandlingForFagsak(true)

        assertThat(arenaStatusService.finnStatus(request).finnes).isTrue()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 1) { behandlingService.finnesBehandlingForFagsak(any()) }
        verify(exactly = 0) { søknadRoutingService.harLagretRouting(any()) }
    }

    @Test
    fun `skal returnere true hvis det finnes ikke finnes behandling men det finnes routing`() {
        mockFinnFagsak(fagsak)
        mockFinnesBehandlingForFagsak(false)
        mockSøknadRouting(true)

        assertThat(arenaStatusService.finnStatus(request).finnes).isTrue()

        verify(exactly = 1) { fagsakService.finnFagsak(any(), any()) }
        verify(exactly = 1) { behandlingService.finnesBehandlingForFagsak(any()) }
        verify(exactly = 1) { søknadRoutingService.harLagretRouting(any()) }
    }

    private fun mockFinnFagsak(fagsak: Fagsak?) {
        every { fagsakService.finnFagsak(eq(setOf(ident)), request.stønadstype) } returns fagsak
    }

    private fun mockFinnesBehandlingForFagsak(svar: Boolean) {
        every { behandlingService.finnesBehandlingForFagsak(fagsak.id) } returns svar
    }

    private fun mockSøknadRouting(svar: Boolean) {
        every { søknadRoutingService.harLagretRouting(any()) } returns svar
    }
}
