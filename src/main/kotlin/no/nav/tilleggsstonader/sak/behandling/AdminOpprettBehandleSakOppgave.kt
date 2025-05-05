package no.nav.tilleggsstonader.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingsjournalpostRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.harStrukturertSøknad
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillJournalføringsoppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandling/admin/fikse-feilede-behandlinger"])
@ProtectedWithClaims(issuer = "azuread")
class AdminOpprettBehandleSakOppgave(
    private val journalpostService: JournalpostService,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingsjournalpostRepository: BehandlingsjournalpostRepository,
    private val transactionHandler: TransactionHandler,
    private val søknadService: SøknadService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val oppgaveClient: OppgaveClient,
) {
    @GetMapping()
    fun hentInfo(): List<String> {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
        return listOf(3859L, 4014, 4027).map {
            val fagsak = fagsakService.hentFagsakPåEksternId(it)
            require(fagsak.stønadstype == Stønadstype.BOUTGIFTER)
            val behandling = behandlingRepository.findByFagsakId(fagsak.id).single()
            val journalpostId = behandlingsjournalpostRepository.findAllByBehandlingId(behandling.id).single().journalpostId
            val journalpost = journalpostService.hentJournalpost(journalpostId)

            val oppgaver =
                oppgaveClient.hentOppgaver(FinnOppgaveRequest(tema = Tema.TSO, journalpostId = journalpostId)).oppgaver

            "fagsak=$it journalpost=${journalpost.journalstatus} oppgaver={${oppgaver.size}}"
        }
    }

    @PostMapping()
    fun fikseFeiledeOpprettBehandling() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)

        transactionHandler.runInTransaction {
            listOf(3859L, 4014, 4027).forEach {
                val fagsak = fagsakService.hentFagsakPåEksternId(it)
                require(fagsak.stønadstype == Stønadstype.BOUTGIFTER)
                val behandling = behandlingRepository.findByFagsakId(fagsak.id).single()
                val journalpostId = behandlingsjournalpostRepository.findAllByBehandlingId(behandling.id).single().journalpostId
                val journalpost = journalpostService.hentJournalpost(journalpostId)
                if (journalpost.harStrukturertSøknad()) {
                    val søknad = journalpostService.hentSøknadFraJournalpost(journalpost, Stønadstype.BOUTGIFTER)
                    søknadService.lagreSøknad(behandling.id, journalpost, søknad)
                }

                ferdigstillJournalføringsoppgave(journalpost, fagsak, journalpostId)

                opprettBehandleSakOppgave(behandling)
            }
        }
    }

    private fun ferdigstillJournalføringsoppgave(
        journalpost: Journalpost,
        fagsak: Fagsak,
        journalpostId: String,
    ) {
        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost,
            emptyMap(),
            emptyMap(),
            ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET,
            fagsak,
            null,
            null,
        )

        val oppgaver =
            oppgaveClient.hentOppgaver(FinnOppgaveRequest(tema = Tema.TSO, journalpostId = journalpostId)).oppgaver
        if (oppgaver.size == 1) {
            taskService.save(FerdigstillJournalføringsoppgaveTask.opprettTask(oppgaver.single().id.toString()))
        }
    }

    private fun opprettBehandleSakOppgave(behandling: Behandling) {
        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    saksbehandler = null, // Behandle sak oppgaven skal være ufordelt
                    beskrivelse = "Skal saksbehandles i ny løsning.",
                ),
            ),
        )
    }
}
