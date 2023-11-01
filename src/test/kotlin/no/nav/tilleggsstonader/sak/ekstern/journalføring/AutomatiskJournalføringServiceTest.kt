package no.nav.tilleggsstonader.sak.ekstern.journalføring

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
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.AutomatiskJournalføringRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AutomatiskJournalføringServiceTest {

    val behandlingService: BehandlingService = mockk()
    val fagsakService: FagsakService = mockk()
    val personService: PersonService = mockk()
    val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    val journalpostService: JournalpostService = mockk()
    val grunnlagsdataService: GrunnlagsdataService = mockk()
    val søknadService: SøknadService = mockk()
    val taskService: TaskService = mockk()

    val automatiskJournalføringService = AutomatiskJournalføringService(
        behandlingService = behandlingService,
        fagsakService = fagsakService,
        personService = personService,
        arbeidsfordelingService = arbeidsfordelingService,
        journalpostService = journalpostService,
        grunnlagsdataService = grunnlagsdataService,
        søknadService = søknadService,
        taskService = taskService,
    )

    val enhet = ArbeidsfordelingService.ENHET_NAY_ROMERIKE.enhetId
    val mappeId = null
    val personIdent = "123456789"
    val aktørId = "9876543210127"
    val tidligerePersonIdent = "9123456789"
    val personIdentAnnen = "9876543210"
    val aktørIdAnnen = "987654321012783123"
    val fagsak = fagsak()
    val journalpostId = "1"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = emptyList(),
        bruker = Bruker(personIdent, BrukerIdType.FNR),
    )

    @BeforeEach
    internal fun setUp() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(
            identer = listOf(
                PdlIdent(personIdent, false),
                PdlIdent(tidligerePersonIdent, false),
            ),
        )
        every { fagsakService.hentEllerOpprettFagsak(any(), any()) } returns fagsak
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns enhet
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
//        every {
//            journalføringService.automatiskJournalfør(
//                any(),
//                any(),
//                any(),
//                any(),
//                any(),
//                any(),
//            )
//        } returns mockk()
    }

    @Test
    internal fun `kan ikke opprette behandling hvis det eksisterer en åpen behandling i ny løsning`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling(status = BehandlingStatus.UTREDES))
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, Stønadstype.BARNETILSYN)
        assertThat(kanOppretteBehandling).isFalse
    }

    @Test
    internal fun `kan opprette behandling hvis det ikke finnes innslag i ny løsning`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf()
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, Stønadstype.BARNETILSYN)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `kan opprette behandling hvis alle behandlinger i ny løsning er henlagt`() {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling(resultat = BehandlingResultat.HENLAGT))
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, Stønadstype.BARNETILSYN)
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
            automatiskJournalføringService.kanOppretteBehandling(personIdent, Stønadstype.BARNETILSYN)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens bruker og personident ikke samsvarer`() {
        val enAnnenBruker = Bruker(
            id = personIdentAnnen,
            type = BrukerIdType.FNR,
        )
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = enAnnenBruker)
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()

        assertThatThrownBy {
            automatiskJournalføringService.håndterSøknad(
                AutomatiskJournalføringRequest(
                    personIdent,
                    journalpostId,
                    Stønadstype.BARNETILSYN,
                ),
            )
        }.hasMessageContaining("Ikke samsvar mellom personident på journalposten")
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens aktørId-bruker og personident ikke samsvarer`() {
        val enAnnenBruker = Bruker(
            id = aktørIdAnnen,
            type = BrukerIdType.AKTOERID,
        )
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = enAnnenBruker)
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(personIdentAnnen, false)))

        assertThatThrownBy {
            automatiskJournalføringService.håndterSøknad(
                AutomatiskJournalføringRequest(
                    personIdent,
                    journalpostId,
                    Stønadstype.BARNETILSYN,
                ),
            )
        }.hasMessageContaining("Ikke samsvar mellom personident på journalposten")
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens bruker mangler`() {
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = null)
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørId, false)))

        assertThatThrownBy {
            automatiskJournalføringService.håndterSøknad(
                AutomatiskJournalføringRequest(
                    personIdent,
                    journalpostId,
                    Stønadstype.BARNETILSYN,
                ),
            )
        }.hasMessageContaining("Journalposten mangler bruker")
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens bruker er orgnr`() {
        val enAnnenBruker = Bruker(
            id = aktørIdAnnen,
            type = BrukerIdType.ORGNR,
        )
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = enAnnenBruker)
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørId, false)))

        assertThatThrownBy {
            automatiskJournalføringService.håndterSøknad(
                AutomatiskJournalføringRequest(
                    personIdent,
                    journalpostId,
                    Stønadstype.BARNETILSYN,
                ),
            )
        }.hasMessageContaining("Ikke samsvar mellom personident på journalposten")
    }

    @Test
    internal fun `skal kunne automatisk journalføre`() {
        val aktørIdBruker = Bruker(
            id = aktørId,
            type = BrukerIdType.AKTOERID,
        )
        val journalpostMedAktørId = journalpost.copy(bruker = aktørIdBruker)
        val taskSlot = slot<Task>()
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpostMedAktørId
        every { journalpostService.oppdaterOgFerdigstillJournalpostMaskinelt(any(), any(), any()) } just Runs
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
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørId, false)))
        every { journalpostService.hentSøknadFraJournalpost(any()) } returns mockk()
        every { søknadService.lagreSøknad(any(), any(), any()) } returns mockk()
        every { taskService.save(capture(taskSlot)) } returns mockk()

        automatiskJournalføringService.håndterSøknad(
            AutomatiskJournalføringRequest(
                personIdent = personIdent,
                journalpostId = journalpostId,
                stønadstype = Stønadstype.BARNETILSYN,
            ),
        )

        verify(exactly = 1) {
            behandlingService.opprettBehandling(
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        }

        assertThat(taskSlot.captured.type).isEqualTo(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
    }
}
