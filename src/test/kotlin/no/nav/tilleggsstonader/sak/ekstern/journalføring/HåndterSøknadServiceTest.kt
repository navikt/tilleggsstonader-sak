package no.nav.tilleggsstonader.sak.ekstern.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.HåndterSøknadRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingTestUtil
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.journalføring.JournalføringService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class HåndterSøknadServiceTest {

    val behandlingService: BehandlingService = mockk()
    val fagsakService: FagsakService = mockk()
    val personService: PersonService = mockk()
    val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    val journalpostService: JournalpostService = mockk()
    val søknadService: SøknadService = mockk()
    val taskService: TaskService = mockk()
    val barnService: BarnService = mockk()
    val journalføringService: JournalføringService = mockk()

    val håndterSøknadService = HåndterSøknadService(
        personService = personService,
        journalpostService = journalpostService,
        taskService = taskService,
        journalføringService = journalføringService,
        fagsakService = fagsakService,
        behandlingService = behandlingService,
        arbeidsfordelingService = arbeidsfordelingService,
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
    internal fun setUp() {
        taskSlot.clear()
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(
            identer = listOf(
                PdlIdent(personIdent, false),
                PdlIdent(tidligerePersonIdent, false),
            ),
        )
        every { fagsakService.hentEllerOpprettFagsak(any(), any()) } returns fagsak
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns enhet
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { søknadService.hentSøknadBarnetilsyn(any()) } returns SøknadBarnetilsyn(
            journalpostId = "",
            mottattTidspunkt = LocalDateTime.MIN,
            data = mockk(),
            barn = setOf(SøknadBarn(ident = "ident", data = mockk())),
            språk = Språkkode.NB,
        )
        every { barnService.opprettBarn(any()) } returns mockk()
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørId, false)))
        every { taskService.save(capture(taskSlot)) } returns mockk()
        every { behandlingService.utledNesteBehandlingstype(fagsak.id) } returns BehandlingType.FØRSTEGANGSBEHANDLING
    }

    @Test
    internal fun `kan ikke opprette behandling hvis det eksisterer en åpen behandling i ny løsning`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling(status = BehandlingStatus.UTREDES))
        val kanOppretteBehandling =
            håndterSøknadService.kanAutomatiskJournalføre(personIdent, Stønadstype.BARNETILSYN)
        assertThat(kanOppretteBehandling).isFalse
    }

    @Test
    internal fun `kan opprette behandling hvis det ikke finnes innslag i ny løsning`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf()
        val kanOppretteBehandling =
            håndterSøknadService.kanAutomatiskJournalføre(personIdent, Stønadstype.BARNETILSYN)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `kan opprette behandling hvis alle behandlinger i ny løsning er henlagt`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(
            behandling(
                resultat = BehandlingResultat.HENLAGT,
                status = BehandlingStatus.FERDIGSTILT,
            ),
        )
        val kanOppretteBehandling =
            håndterSøknadService.kanAutomatiskJournalføre(personIdent, Stønadstype.BARNETILSYN)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `kan opprette behandling hvis det finnes innslag i ny løsning der alle er ferdigstilt`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(
            behandling(
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
            ),
            behandling(resultat = BehandlingResultat.AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
        )
        val kanOppretteBehandling =
            håndterSøknadService.kanAutomatiskJournalføre(personIdent, Stønadstype.BARNETILSYN)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `skal kunne automatisk journalføre`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()

        justRun { journalføringService.journalførTilNyBehandling(journalpostId, personIdent, Stønadstype.BARNETILSYN, any(), any(), any()) }

        håndterSøknadService.håndterSøknad(
            HåndterSøknadRequest(
                personIdent = personIdent,
                journalpostId = journalpostId,
                stønadstype = Stønadstype.BARNETILSYN,
            ),
        )

        verify(exactly = 1) {
            journalføringService.journalførTilNyBehandling(
                journalpostId,
                personIdent,
                Stønadstype.BARNETILSYN,
                BehandlingÅrsak.SØKNAD,
                "Automatisk journalført søknad. Skal saksbehandles i ny løsning.",
                enhet,
            )
        }
    }

    @Test
    fun `skal opprette journalføringsoppgave hvis man ikke kan automatisk journalføre`() {
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling())

        håndterSøknadService.håndterSøknad(
            HåndterSøknadRequest(
                personIdent,
                journalpostId,
                Stønadstype.BARNETILSYN,
            ),
        )

        assertThat(taskSlot.captured.type).isEqualTo(OpprettOppgaveTask.TYPE)
        val payload = objectMapper.readValue<OpprettOppgaveTask.OpprettOppgaveTaskData>(taskSlot.captured.payload)
        assertThat(payload.oppgave.journalpostId).isEqualTo(journalpostId)
        assertThat(payload.oppgave.oppgavetype).isEqualTo(Oppgavetype.Journalføring)
        assertThat(payload.oppgave.enhetsnummer).isEqualTo("123")
    }
}
