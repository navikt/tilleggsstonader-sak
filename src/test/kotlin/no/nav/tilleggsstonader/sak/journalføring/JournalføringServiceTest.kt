package no.nav.tilleggsstonader.sak.journalføring

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JournalføringServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val journalpostService = mockk<JournalpostService>()
    val søknadService = mockk<SøknadService>()
    val taskService = mockk<TaskService>()
    val barnService = mockk<BarnService>()

    val journalføringService = JournalføringService(
        behandlingService,
        fagsakService,
        journalpostService,
        søknadService,
        taskService,
        barnService,
        TransactionHandler()
    )

    val enhet = ArbeidsfordelingService.ENHET_NASJONAL_NAY.enhetId
    val personIdent = "123456789"
    val aktørId = "9876543210127"
    val tidligerePersonIdent = "9123456789"
    val fagsak = fagsak()
    val journalpostId = "1"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = listOf(DokumentInfo("", brevkode = "1")),
        bruker = Bruker(personIdent, BrukerIdType.FNR),
        journalforendeEnhet = "123",
    )

    val taskSlot = slot<Task>()

    @BeforeEach
    fun setUp() {
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { fagsakService.hentEllerOpprettFagsak(any(), any()) } returns fagsak
        every { behandlingService.utledNesteBehandlingstype(fagsak.id) } returns BehandlingType.FØRSTEGANGSBEHANDLING
        every { taskService.save(capture(taskSlot)) } returns mockk()
    }

    @AfterEach
    fun tearDown() {
        taskSlot.clear()
    }

    @Test
    internal fun `skal ikke kunne journalføre hvis journalpostens bruker mangler`() {
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = null)
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()

        Assertions.assertThatThrownBy {
            journalføringService.journalførTilNyBehandling(
                journalpostId,
                personIdent,
                Stønadstype.BARNETILSYN,
                BehandlingÅrsak.SØKNAD,
                "oppgaveBeskrivelse",
                enhet,
            )
        }.hasMessageContaining("Journalposten mangler bruker")
    }

    @Test
    internal fun `skal kunne journalføre og opprette behandling`() {
        val aktørIdBruker = Bruker(
            id = aktørId,
            type = BrukerIdType.AKTOERID,
        )
        val journalpostMedAktørId = journalpost.copy(bruker = aktørIdBruker)
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpostMedAktørId
        every { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any()) } just Runs
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak

        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { behandlingService.leggTilBehandlingsjournalpost(any(), any(), any()) } just Runs
        every {
            behandlingService.opprettBehandling(
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        } returns behandling(fagsak = fagsak)
        every { journalpostService.hentSøknadFraJournalpost(any()) } returns mockk()
        every { søknadService.lagreSøknad(any(), any(), any()) } returns mockk()

        journalføringService.journalførTilNyBehandling(
            journalpostId,
            personIdent,
            Stønadstype.BARNETILSYN,
            BehandlingÅrsak.SØKNAD,
            "beskrivelse",
            enhet,
        )

        verify(exactly = 1) {
            behandlingService.opprettBehandling(
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        }
        verify(exactly = 1) {
            journalpostService.oppdaterOgFerdigstillJournalpost(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
    }
}
