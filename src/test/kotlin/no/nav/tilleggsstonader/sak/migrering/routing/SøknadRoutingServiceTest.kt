package no.nav.tilleggsstonader.sak.migrering.routing

import io.getunleash.variant.Variant
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockGetVariant
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingServiceTest.MockToggle.SØKNAD_ROUTING_BOUTGIFTER
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaStatusDtoUtil.vedtakStatus
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

class SøknadRoutingServiceTest {
    val søknadRoutingRepository = mockk<SøknadRoutingRepository>()
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val arenaService = mockk<ArenaService>()
    val personService = mockk<PersonService>()
    val unleashService = mockUnleashService()

    private val service =
        SøknadRoutingService(
            søknadRoutingRepository,
            fagsakService,
            behandlingService,
            arenaService,
            unleashService,
        )

    private enum class MockToggle(
        override val toggleId: String,
    ) : ToggleId {
        SØKNAD_ROUTING_BOUTGIFTER("toggleId"),
    }

    private val ident = "1"
    private val stønadstype = Stønadstype.BOUTGIFTER

    private val søknadRouting = SøknadRouting(ident = ident, type = stønadstype, detaljer = JsonWrapper(""))

    private val skalRouteAlleSøkereTilNyLøsning = SkalRouteAlleSøkereTilNyLøsning(ident, stønadstype)
    private val featureToggletHarGyldigStateIArena =
        FeatureTogglet(
            ident = ident,
            stønadstype = stønadstype,
            toggleId = SØKNAD_ROUTING_BOUTGIFTER,
            harGyldigStateIArena = { true },
        )

    private val featureToggletHarUgyldigStateIArena =
        FeatureTogglet(
            ident = ident,
            stønadstype = stønadstype,
            toggleId = SØKNAD_ROUTING_BOUTGIFTER,
            harGyldigStateIArena = { false },
        )

    @BeforeEach
    fun setUp() {
        mockkObject(EnvUtil)
        every { EnvUtil.erIDev() } returns true

        every { søknadRoutingRepository.findByIdentAndType(any(), any()) } returns null
        every { søknadRoutingRepository.countByType(any()) } returns 10
        every { søknadRoutingRepository.insert(any()) } answers { firstArg() }
        every { fagsakService.finnFagsak(any(), any()) } returns null
        every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns emptyList()
        every { arenaService.hentStatus(any(), any()) } returns arenaStatus()
        every { personService.hentFolkeregisterIdenter(any()) } answers
            { PdlIdenter(listOf(PdlIdent(firstArg(), false, "FOLKEREGISTERIDENT"))) }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(EnvUtil)
        clearMocks(unleashService)
    }

    @Test
    fun `skal returnere true hvis det finnes innslag i databasen`() {
        every { søknadRoutingRepository.findByIdentAndType(ident, stønadstype) } returns søknadRouting

        assertThat(skalBehandlesINyLøsning()).isTrue

        verify {
            søknadRoutingRepository.findByIdentAndType(ident, stønadstype)

            fagsakService wasNot called
            behandlingService wasNot called
            arenaService wasNot called
        }
        verify(exactly = 0) {
            søknadRoutingRepository.insert(any())
        }
    }

    @Nested
    inner class HarBehandling {
        @Test
        fun `skal sjekke om det finnes behandling hvis det ikke finnes innslag i databasen`() {
            unleashService.mockGetVariant(SØKNAD_ROUTING_BOUTGIFTER, søknadRoutingVariant(0))
            assertThat(skalBehandlesINyLøsning(featureToggletHarGyldigStateIArena)).isFalse

            verify {
                fagsakService.finnFagsak(any(), any())
                behandlingService wasNot called
            }
            verify(exactly = 0) {
                søknadRoutingRepository.insert(any())
            }
        }

        @Test
        fun `skal oppdatere reopository hvis det finnes en behandling`() {
            val fagsak = fagsak()
            every { fagsakService.finnFagsak(any(), any()) } returns fagsak
            every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling())

            assertThat(skalBehandlesINyLøsning(featureToggletHarGyldigStateIArena)).isTrue

            verify {
                fagsakService.finnFagsak(any(), any())
                behandlingService.hentBehandlinger(any<FagsakId>())
                søknadRoutingRepository.insert(any())
                arenaService wasNot called
            }
        }
    }

    @Nested
    inner class ArenaStatus {
        @BeforeEach
        fun setUp() {
            unleashService.mockGetVariant(SØKNAD_ROUTING_BOUTGIFTER, søknadRoutingVariant(20))
        }

        @Test
        fun `skal ikke route bruker hvis bruker har ugyldig state i Arena`() {
            assertThat(skalBehandlesINyLøsning(featureToggletHarUgyldigStateIArena)).isFalse

            verify {
                arenaService.hentStatus(any(), any())
            }
            verify(exactly = 0) {
                søknadRoutingRepository.insert(any())
            }
        }

        @Test
        fun `skal route bruker hvis bruker har gyldig state i Arena`() {
            assertThat(skalBehandlesINyLøsning(featureToggletHarGyldigStateIArena)).isTrue

            verify {
                fagsakService.finnFagsak(any(), any())
                arenaService.hentStatus(any(), any())
                søknadRoutingRepository.insert(any())
                behandlingService wasNot called
            }
        }

        @Test
        fun `skal route hvis det ikke er aktivt vedtak`() {
            assertThat(skalBehandlesINyLøsning(featureToggletHarGyldigStateIArena)).isTrue

            verify {
                fagsakService.finnFagsak(any(), any())
                arenaService.hentStatus(any(), any())
                søknadRoutingRepository.insert(any())
                behandlingService wasNot called
            }
        }
    }

    @Nested
    inner class Toggle {
        @Test
        fun `skal behandles i ny løsning dersom maks antall ikke er nådd`() {
            unleashService.mockGetVariant(SØKNAD_ROUTING_BOUTGIFTER, søknadRoutingVariant(10))
            every { søknadRoutingRepository.countByType(Stønadstype.BOUTGIFTER) } returns 0

            assertThat(skalBehandlesINyLøsning(featureToggletHarGyldigStateIArena)).isTrue

            verify {
                unleashService.getVariant(SØKNAD_ROUTING_BOUTGIFTER)
                søknadRoutingRepository.countByType(Stønadstype.BOUTGIFTER)
                søknadRoutingRepository.insert(any())
            }
        }

        @Test
        fun `skal ikke behandles i ny løsning dersom maks antall er nådd`() {
            unleashService.mockGetVariant(SØKNAD_ROUTING_BOUTGIFTER, søknadRoutingVariant(0))
            every { søknadRoutingRepository.countByType(Stønadstype.BOUTGIFTER) } returns 0

            assertThat(skalBehandlesINyLøsning(featureToggletHarGyldigStateIArena)).isFalse

            verify {
                unleashService.getVariant(SØKNAD_ROUTING_BOUTGIFTER)
                søknadRoutingRepository.countByType(Stønadstype.BOUTGIFTER)
            }
            verify(exactly = 0) { søknadRoutingRepository.insert(any()) }
        }
    }

    @Nested
    inner class SkalRuteAlleUtenÅSjekkeFeatureToggle {
        @Test
        fun `skal rute alle til ny løsning`() {
            val resultat = skalBehandlesINyLøsning(skalRouteAlleSøkereTilNyLøsning)
            assertThat(resultat).isTrue()

            verify {
                søknadRoutingRepository.findByIdentAndType(ident, stønadstype)

                fagsakService wasNot called
                behandlingService wasNot called
                arenaService wasNot called
            }
            verify(exactly = 1) {
                søknadRoutingRepository.insert(any())
            }
        }
    }

    private fun arenaStatus() =
        ArenaStatusDto(
            SakStatus(harAktivSakUtenVedtak = false),
            vedtakStatus(harVedtak = false, harAktivtVedtak = false, harVedtakUtenUtfall = false),
        )

    private fun skalBehandlesINyLøsning(request: IdentStønadstype = IdentStønadstype(ident, stønadstype)) =
        service.sjekkRoutingForPerson(request.tilRoutingContext()).skalBehandlesINyLøsning

    private fun skalBehandlesINyLøsning(context: RoutingContext) = service.sjekkRoutingForPerson(context).skalBehandlesINyLøsning

    private fun søknadRoutingVariant(
        antall: Int = 1000,
        enabled: Boolean = true,
    ): Variant = Variant("antall", antall.toString(), enabled)
}
