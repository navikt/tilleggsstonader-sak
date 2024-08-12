package no.nav.tilleggsstonader.sak.journalføring

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingTestUtil
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
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
    val personService = mockk<PersonService>()
    val oppgaveService = mockk<OppgaveService>()
    val unleashService = mockk<UnleashService>()

    val journalføringService = JournalføringService(
        behandlingService,
        fagsakService,
        journalpostService,
        søknadService,
        taskService,
        barnService,
        TransactionHandler(),
        personService,
        oppgaveService,
        unleashService,
    )

    val enhet = ArbeidsfordelingTestUtil.ENHET_NASJONAL_NAY.enhetNr
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
        every { taskService.save(capture(taskSlot)) } returns mockk()
        every { personService.hentPersonIdenter(personIdent) } returns PdlIdenter(listOf(PdlIdent(personIdent, false)))
        justRun { oppgaveService.ferdigstillOppgave(any()) }
        every { journalpostService.hentIdentFraJournalpost(journalpost) } returns personIdent
        every { unleashService.isEnabled(any()) } returns true
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
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost
        every { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any(), any()) } just Runs
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak

        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { behandlingService.leggTilBehandlingsjournalpost(any(), any(), any()) } just Runs
        every {
            behandlingService.opprettBehandling(
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        } returns behandling(fagsak = fagsak)
        every { journalpostService.hentSøknadFraJournalpost(any()) } returns mockk()
        every { søknadService.lagreSøknad(any(), any(), any()) } returns mockk()
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns mockk()

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
                any(),
            )
        }

        Assertions.assertThat(taskSlot.captured.type).isEqualTo(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
    }

    @Test
    fun `skal oppdatere journalpost med avsender fra journalføringsrequest`() {
        val nyAvsender = JournalføringRequest.NyAvsender(true, "navn", personIdent)

        val journalføringRequest = JournalføringRequest(
            stønadstype = Stønadstype.BARNETILSYN,
            oppgaveId = "1",
            journalførendeEnhet = "123",
            årsak = JournalføringRequest.Journalføringsårsak.PAPIRSØKNAD,
            aksjon = JournalføringRequest.Journalføringsaksjon.JOURNALFØR_PÅ_FAGSAK,
            nyAvsender = nyAvsender,
        )

        val nyAvsenderSlot = slot<AvsenderMottaker?>()

        every { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any(), any(), captureNullable(nyAvsenderSlot)) } just Runs

        journalføringService.fullførJournalpost(journalføringRequest, journalpost)

        Assertions.assertThat(nyAvsenderSlot.captured!!.navn).isEqualTo(nyAvsender.navn)
        Assertions.assertThat(nyAvsenderSlot.captured!!.id).isEqualTo(nyAvsender.personIdent)
        Assertions.assertThat(nyAvsenderSlot.captured!!.idType).isEqualTo(BrukerIdType.FNR)
    }
}
