package no.nav.tilleggsstonader.sak.behandling

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak.FEILREGISTRERT
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Endret
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingServiceTest {

    private val behandlingRepository: BehandlingRepository = mockk()
    private val behandlingshistorikkService: BehandlingshistorikkService = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)

    private val behandlingService =
        BehandlingService(
            behandlingsjournalpostRepository = mockk(),
            behandlingRepository = behandlingRepository,
            eksternBehandlingIdRepository = mockk(),
            behandlingshistorikkService = behandlingshistorikkService,
            taskService = taskService,
            unleashService = mockUnleashService(),
        )

    private val behandlingSlot = slot<Behandling>()

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
        every {
            behandlingRepository.update(capture(behandlingSlot))
        } answers {
            behandlingSlot.captured
        }
        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
    }

    @AfterAll
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks(answers = false)
    }

    @Test
    internal fun `skal feile hvis krav mottatt er frem i tid`() {
        assertThrows<ApiFeil> {
            behandlingService.opprettBehandling(
                status = BehandlingStatus.OPPRETTET,
                stegType = StegType.VILKÅR,
                behandlingsårsak = BehandlingÅrsak.PAPIRSØKNAD,
                kravMottatt = osloDateNow().plusDays(1),
                fagsakId = FagsakId.randomUUID(),
            )
        }
    }

    @Nested
    inner class HenleggBehandling {

        @Test
        internal fun `skal kunne henlegge behandling som er førstegangsbehandling`() {
            val behandling =
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, henlagtÅrsak = FEILREGISTRERT)
        }

        @Test
        internal fun `skal kunne henlegge behandling som er revurdering`() {
            val behandling = behandling(fagsak(), type = BehandlingType.REVURDERING, status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, FEILREGISTRERT)
        }

        private fun henleggOgForventOk(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(behandlingSlot.captured.resultat).isEqualTo(BehandlingResultat.HENLAGT)
            assertThat(behandlingSlot.captured.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingSlot.captured.vedtakstidspunkt).isNotNull
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling hvor vedtak fattes`() {
            val behandling =
                behandling(
                    fagsak(),
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.FATTER_VEDTAK,
                )
            henleggOgForventApiFeilmelding(behandling, FEILREGISTRERT)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er iverksatt`() {
            val behandling = behandling(
                fagsak(),
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                status = BehandlingStatus.IVERKSETTER_VEDTAK,
            )
            henleggOgForventApiFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er ferdigstilt`() {
            val behandling =
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FERDIGSTILT)
            henleggOgForventApiFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        private fun henleggOgForventApiFeilmelding(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            val feil: ApiFeil = assertThrows {
                behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class OppdaterResultatPåBehandling {

        private val behandling = behandling()

        @Test
        internal fun `skal sette vedtakstidspunkt når man setter resultat`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(resultat = BehandlingResultat.IKKE_SATT)
            behandlingService.oppdaterResultatPåBehandling(BehandlingId.randomUUID(), BehandlingResultat.INNVILGET)
        }

        @Test
        internal fun `skal feile hvis resultatet på behandlingen allerede er satt`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(resultat = BehandlingResultat.INNVILGET)
            assertThatThrownBy {
                behandlingService.oppdaterResultatPåBehandling(BehandlingId.randomUUID(), BehandlingResultat.INNVILGET)
            }.hasMessageContaining("Kan ikke endre resultat på behandling når resultat")
        }

        @Test
        internal fun `skal feile hvis man setter IKKE_SATT`() {
            assertThatThrownBy {
                behandlingService.oppdaterResultatPåBehandling(BehandlingId.randomUUID(), BehandlingResultat.IKKE_SATT)
            }.hasMessageContaining("Må sette et endelig resultat")
        }
    }

    @Nested
    inner class HentBehandlinger {

        @Test
        internal fun `skal sortere behandlinger etter vedtakstidspunkt og til sist uten vedtakstidspunkt`() {
            val tiDagerSiden = osloNow().minusDays(10)
            val femFagerSiden = osloNow().minusDays(5)
            val now = osloNow()
            val behandling1 = opprettBehandling(femFagerSiden, tiDagerSiden, tiDagerSiden)
            val behandling2 = opprettBehandling(null, femFagerSiden, femFagerSiden)
            val behandling3 = opprettBehandling(tiDagerSiden, now, now)
            every {
                behandlingRepository.findByFagsakId(any())
            } returns listOf(behandling1, behandling2, behandling3)

            val hentBehandlinger = behandlingService.hentBehandlinger(FagsakId.randomUUID())
            assertThat(hentBehandlinger)
                .containsExactly(behandling3, behandling1, behandling2)
        }

        private fun opprettBehandling(
            vedtakstidspunkt: LocalDateTime?,
            opprettetTid: LocalDateTime,
            endretTid: LocalDateTime,
        ) = behandling(vedtakstidspunkt = vedtakstidspunkt).copy(
            sporbar = Sporbar(
                opprettetTid = opprettetTid,
                endret = Endret(endretTid = endretTid),
            ),
        )
    }

    @Nested
    inner class UtledNesteBehandlingstype {

        @Test
        internal fun `skal returnere revurdering hvis det finnes en førstegangsbehandling`() {
            val fagsak = fagsak()
            every {
                behandlingRepository.findByFagsakId(fagsak.id)
            } returns listOf(
                behandling(
                    fagsak,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    resultat = BehandlingResultat.INNVILGET,
                ),
            )

            assertThat(behandlingService.utledNesteBehandlingstype(fagsak.id)).isEqualTo(BehandlingType.REVURDERING)
        }

        @Test
        internal fun `skal returnere førstegangsbehandling hvis det ikke finnes en førstegangsbehandling`() {
            val fagsak = fagsak()
            every {
                behandlingRepository.findByFagsakId(fagsak.id)
            } returns emptyList()

            assertThat(behandlingService.utledNesteBehandlingstype(fagsak.id)).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        }

        @Test
        internal fun `skal returnere førstegangsbehandling hvis det finnes en førstegangsbehandling som er henlagt`() {
            val fagsak = fagsak()
            every {
                behandlingRepository.findByFagsakId(fagsak.id)
            } returns listOf(
                behandling(
                    fagsak,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    resultat = BehandlingResultat.HENLAGT,
                ),
            )

            assertThat(behandlingService.utledNesteBehandlingstype(fagsak.id)).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        }

        @Test
        internal fun `skal returnere revurdering hvis det finnes en førstegangsbehandling som ikke er ferdigstilt`() {
            val fagsak = fagsak()
            every {
                behandlingRepository.findByFagsakId(fagsak.id)
            } returns listOf(
                behandling(
                    fagsak,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.UTREDES,
                    resultat = BehandlingResultat.IKKE_SATT,
                ),
            )

            assertThat(behandlingService.utledNesteBehandlingstype(fagsak.id)).isEqualTo(BehandlingType.REVURDERING)
        }
    }
}
