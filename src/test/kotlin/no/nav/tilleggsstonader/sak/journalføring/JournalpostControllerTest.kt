package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.fullførJournalpost
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.klage.KlageClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class JournalpostControllerTest : IntegrationTest() {
    val ident = "12345678910"
    val saksbehandler = "ole"
    val enhet = "enhet"

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var klageClient: KlageClient

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettPerson(ident)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        mockService.resetJournalpostClient()
        mockService.resetOppgaveClient()
    }

    @Test
    fun `fullfør journalpost - skal ferdigstille journalpost, og opprette behandling og oppgave`() {
        val journalpostId =
            medBrukercontext(bruker = saksbehandler) {
                fullførJournalpost(
                    "1",
                    JournalføringRequest(
                        stønadstype = Stønadstype.BARNETILSYN,
                        aksjon = JournalføringRequest.Journalføringsaksjon.OPPRETT_BEHANDLING,
                        årsak = JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD,
                        oppgaveId = "123",
                        journalførendeEnhet = enhet,
                        logiskeVedlegg = mapOf("1" to listOf(LogiskVedlegg("1", "ny tittel"))),
                    ),
                )
            }

        assertThat(journalpostId).isEqualTo("1")

        val fagsak = fagsakService.finnFagsak(setOf(ident), Stønadstype.BARNETILSYN)
        assertThat(fagsak).isNotNull

        val behandlinger = behandlingService.hentBehandlinger(fagsak!!.id)
        assertThat(behandlinger).hasSize(1)

        val opprettetBehandling = behandlinger.first()
        assertThat(opprettetBehandling.årsak).isEqualTo(JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD.behandlingsårsak)
        assertThat(opprettetBehandling.steg).isEqualTo(StegType.INNGANGSVILKÅR)
        assertThat(opprettetBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

        val opprettedeTasks = taskService.findAll()
        assertThat(opprettedeTasks).hasSize(2)

        val bahandlesakOppgaveTask = opprettedeTasks.single { it.type == OpprettOppgaveForOpprettetBehandlingTask.TYPE }
        val behandlesakOppgavePayload =
            ObjectMapperProvider.objectMapper.readValue<OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData>(
                bahandlesakOppgaveTask.payload,
            )
        assertThat(behandlesakOppgavePayload.behandlingId).isEqualTo(opprettetBehandling.id)

        verify(exactly = 1) { mockService.journalpostClient.ferdigstillJournalpost("1", enhet, saksbehandler) }
        verify(exactly = 1) {
            mockService.journalpostClient.oppdaterLogiskeVedlegg(
                "1",
                BulkOppdaterLogiskVedleggRequest(listOf("ny tittel")),
            )
        }
        verify(exactly = 1) { mockService.oppgaveClient.ferdigstillOppgave("123".toLong()) }
    }

    @Test
    fun `fullfør journalpost - skal ferdigstille journalpost, og opprette klage`() {
        val journalpostId =
            medBrukercontext(bruker = saksbehandler) {
                fullførJournalpost(
                    "1",
                    JournalføringRequest(
                        stønadstype = Stønadstype.BARNETILSYN,
                        aksjon = JournalføringRequest.Journalføringsaksjon.OPPRETT_BEHANDLING,
                        årsak = JournalføringRequest.Journalføringsårsak.KLAGE,
                        oppgaveId = "123",
                        journalførendeEnhet = enhet,
                        logiskeVedlegg = mapOf("1" to listOf(LogiskVedlegg("1", "ny tittel"))),
                    ),
                )
            }

        assertThat(journalpostId).isEqualTo("1")

        val fagsak = fagsakService.finnFagsak(setOf(ident), Stønadstype.BARNETILSYN)
        assertThat(fagsak).isNotNull

        val behandlinger = behandlingService.hentBehandlinger(fagsak!!.id)
        assertThat(behandlinger).hasSize(0)

        verify(exactly = 1) {
            klageClient.opprettKlage(
                OpprettKlagebehandlingRequest(
                    ident = "12345678910",
                    stønadstype = Stønadstype.BARNETILSYN,
                    eksternFagsakId = fagsak.eksternId.id.toString(),
                    fagsystem = Fagsystem.TILLEGGSSTONADER,
                    klageMottatt = LocalDate.now().minusDays(7),
                    behandlendeEnhet = "4462",
                ),
            )
        }

        verify(exactly = 1) { mockService.journalpostClient.ferdigstillJournalpost("1", enhet, saksbehandler) }
        verify(exactly = 1) {
            mockService.journalpostClient.oppdaterLogiskeVedlegg(
                "1",
                BulkOppdaterLogiskVedleggRequest(listOf("ny tittel")),
            )
        }
        verify(exactly = 1) { mockService.oppgaveClient.ferdigstillOppgave("123".toLong()) }
    }
}
