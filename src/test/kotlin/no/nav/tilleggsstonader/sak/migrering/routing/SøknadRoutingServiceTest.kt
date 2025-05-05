package no.nav.tilleggsstonader.sak.migrering.routing

import io.getunleash.variant.Variant
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.arena.SakStatus
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle.SØKNAD_ROUTING_LÆREMIDLER
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockGetVariant
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
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

    private val ident = "1"
    private val stønadstype = Stønadstype.BARNETILSYN

    private val søknadRouting = SøknadRouting(ident = ident, type = stønadstype, detaljer = JsonWrapper(""))

    @BeforeEach
    fun setUp() {
        mockkObject(EnvUtil)
        every { EnvUtil.erIDev() } returns true

        every { søknadRoutingRepository.findByIdentAndType(any(), any()) } returns null
        every { søknadRoutingRepository.countByType(any()) } returns 10
        every { søknadRoutingRepository.insert(any()) } answers { firstArg() }
        every { fagsakService.finnFagsak(any(), any()) } returns null
        every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns emptyList()
        every { arenaService.hentStatus(any(), any()) } returns arenaStatusAktivtVedtak()
        every { personService.hentFolkeregisterIdenter(any()) } answers
            { PdlIdenter(listOf(PdlIdent(firstArg(), false, "FOLKEREGISTERIDENT"))) }
        unmockkObject(unleashService)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(EnvUtil)
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
        fun `skal sjekke om det finnes behandling hvis det ikke innslag i databasen`() {
            assertThat(skalBehandlesINyLøsning()).isFalse

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

            assertThat(skalBehandlesINyLøsning()).isTrue

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
        @Test
        fun `skal sjekke arenstatus hvis det ikke finnes innslag i databasen eller behandlinger`() {
            assertThat(skalBehandlesINyLøsning()).isFalse

            verify {
                arenaService.hentStatus(any(), any())
            }
            verify(exactly = 0) {
                søknadRoutingRepository.insert(any())
            }
        }

        @Test
        fun `skal oppdatere databasen hvis statusen i arena tilsier sånn`() {
            every { arenaService.hentStatus(any(), any()) } returns arenaStatusKanRoutes()

            assertThat(skalBehandlesINyLøsning()).isTrue

            verify {
                fagsakService.finnFagsak(any(), any())
                arenaService.hentStatus(any(), any())
                søknadRoutingRepository.insert(any())
                behandlingService wasNot called
            }
        }

        @Test
        fun `skal route hvis det ikke er aktivt vedtak`() {
            every { arenaService.hentStatus(any(), any()) } returns arenaStatusUtenAktivtVedtak()

            assertThat(skalBehandlesINyLøsning()).isTrue

            verify {
                fagsakService.finnFagsak(any(), any())
                arenaService.hentStatus(any(), any())
                søknadRoutingRepository.insert(any())
                behandlingService wasNot called
            }
        }

        @Test
        fun `skal route hvis det er vedtak uten utfall`() {
            every { arenaService.hentStatus(any(), any()) } returns arenaStatusVedtakUtenUtfall()

            assertThat(skalBehandlesINyLøsning()).isTrue

            verify(exactly = 1) {
                søknadRoutingRepository.insert(any())
            }
        }

        @Test
        fun `skal ikke route læremidler dersom det finnes noe aktivt vedtak`() {
            unleashService.mockGetVariant(SØKNAD_ROUTING_LÆREMIDLER, søknadRoutingVariant(10))
            every { arenaService.hentStatus(any(), any()) } returns arenaStatusAktivtVedtak()
            every { søknadRoutingRepository.countByType(Stønadstype.LÆREMIDLER) } returns 0

            assertThat(skalBehandlesINyLøsning(IdentStønadstype(ident, Stønadstype.LÆREMIDLER))).isFalse

            verify(exactly = 0) {
                søknadRoutingRepository.insert(any())
            }
            verify(exactly = 1) {
                arenaService.hentStatus(any(), any())
            }
        }
    }

    @Nested
    inner class Toggle {
        @Test
        fun `skal behandles i ny løsning dersom maks antall ikke er nådd`() {
            unleashService.mockGetVariant(SØKNAD_ROUTING_LÆREMIDLER, søknadRoutingVariant(10))
            every { søknadRoutingRepository.countByType(Stønadstype.LÆREMIDLER) } returns 0
            every { arenaService.hentStatus(any(), any()) } returns arenaStatusKanRoutes()

            assertThat(skalBehandlesINyLøsning(IdentStønadstype(ident, Stønadstype.LÆREMIDLER))).isTrue

            verify {
                unleashService.getVariant(SØKNAD_ROUTING_LÆREMIDLER)
                søknadRoutingRepository.countByType(Stønadstype.LÆREMIDLER)
                søknadRoutingRepository.insert(any())
            }
        }

        @Test
        fun `skal ikke behandles i ny løsning dersom maks antall er nådd`() {
            unleashService.mockGetVariant(SØKNAD_ROUTING_LÆREMIDLER, søknadRoutingVariant(0))
            every { søknadRoutingRepository.countByType(Stønadstype.LÆREMIDLER) } returns 0
            every { arenaService.hentStatus(any(), any()) } returns arenaStatusKanRoutes()

            assertThat(skalBehandlesINyLøsning(IdentStønadstype(ident, Stønadstype.LÆREMIDLER))).isFalse

            verify {
                unleashService.getVariant(SØKNAD_ROUTING_LÆREMIDLER)
                søknadRoutingRepository.countByType(Stønadstype.LÆREMIDLER)
            }
            verify(exactly = 0) { søknadRoutingRepository.insert(any()) }
        }

        @Test
        fun `skal ikke sjekke toggle for tilsyn barn`() {
            assertThat(skalBehandlesINyLøsning()).isFalse

            verify(exactly = 0) {
                unleashService.getVariant(any())
                søknadRoutingRepository.countByType(any())
            }
        }
    }

    @Nested
    inner class SkalRuteAlleUtenÅSjekkeFeatureToggle {
        @Test
        fun `skal rute alle til ny løsning`() {
            val resultat = skalBehandlesINyLøsning(sjekkSkalRuteAlleSøkere = true)
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

    private fun arenaStatusAktivtVedtak() =
        ArenaStatusDto(
            SakStatus(harAktivSakUtenVedtak = true),
            vedtakStatus(harVedtak = true, harAktivtVedtak = true, harVedtakUtenUtfall = false),
        )

    private fun arenaStatusKanRoutes() =
        ArenaStatusDto(
            SakStatus(harAktivSakUtenVedtak = false),
            vedtakStatus(harVedtak = false, harAktivtVedtak = false, harVedtakUtenUtfall = false),
        )

    private fun arenaStatusUtenAktivtVedtak() =
        ArenaStatusDto(
            SakStatus(harAktivSakUtenVedtak = false),
            vedtakStatus(harVedtak = true, harAktivtVedtak = false, harVedtakUtenUtfall = false),
        )

    private fun arenaStatusVedtakUtenUtfall() =
        ArenaStatusDto(
            SakStatus(harAktivSakUtenVedtak = false),
            vedtakStatus(harVedtak = true, harAktivtVedtak = false, harVedtakUtenUtfall = true),
        )

    private fun skalBehandlesINyLøsning(
        request: IdentStønadstype = IdentStønadstype(ident, stønadstype),
        sjekkSkalRuteAlleSøkere: Boolean = false,
    ) = service.sjekkRoutingForPerson(request, sjekkSkalRuteAlleSøkere).skalBehandlesINyLøsning

    private fun søknadRoutingVariant(
        antall: Int = 1000,
        enabled: Boolean = true,
    ): Variant = Variant("antall", antall.toString(), enabled)
}
