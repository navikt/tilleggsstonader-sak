package no.nav.tilleggsstonader.sak.journalføring

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalpostResponse
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod

class JournalpostControllerTest: IntegrationTest() {

    val ident = "123456789"
    val saksbehandler = "ole"
    val enhet = "enhet"

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var journalpostClient: JournalpostClient



    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken(saksbehandler = saksbehandler))
        testoppsettService.opprettPerson(ident)
    }


    @Test
    fun `fullfør journalpost - skal ferdigstille journalpost, og opprette behandling og oppgave`() {
        val journalpostId = fullførJournalpost(
            "1",
            JournalføringRequest(
                stønadstype = Stønadstype.BARNETILSYN,
                ident = ident,
                aksjon = JournalføringRequest.Journalføringsaksjon.OPPRETT_BEHANDLING,
                årsak = JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD,
                oppgaveId = "123",
                journalførendeEnhet = enhet
            )
        )

        assertThat(journalpostId).isEqualTo("1")

        val fagsak = fagsakService.finnFagsak(setOf(ident), Stønadstype.BARNETILSYN)
        assertThat(fagsak).isNotNull

        val behandlinger = behandlingService.hentBehandlinger(fagsak!!.id)
        assertThat(behandlinger).hasSize(1)

        val opprettetBehandling = behandlinger.first()
        assertThat(opprettetBehandling.årsak).isEqualTo(JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD.behandlingsårsak)
        assertThat(opprettetBehandling.steg).isEqualTo(StegType.INNGANGSVILKÅR)
        assertThat(opprettetBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

        assertThat(taskService.findAll().single().type).isEqualTo(OpprettOppgaveForOpprettetBehandlingTask.TYPE)

        verify(exactly = 1) { journalpostClient.ferdigstillJournalpost("1", enhet, saksbehandler) }

    }


    private fun fullførJournalpost(journalpostId: String, request: JournalføringRequest): String =
        restTemplate.exchange(
            localhost("api/journalpost/$journalpostId/fullfor"),
            HttpMethod.POST,
            HttpEntity(request, headers),
            String::class.java
        ).body!!

}