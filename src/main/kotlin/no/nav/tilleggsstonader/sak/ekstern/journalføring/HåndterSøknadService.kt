package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.HåndterSøknadRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.journalføring.JournalføringService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HåndterSøknadService(
    private val personService: PersonService,
    private val journalpostService: JournalpostService,
    private val taskService: TaskService,
    private val journalføringService: JournalføringService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) {
    @Transactional
    fun håndterSøknad(request: HåndterSøknadRequest) {
        val personIdent = request.personIdent
        val stønadstype = request.stønadstype

        val journalpost = journalpostService.hentJournalpost(request.journalpostId)
        håndterSøknad(personIdent = personIdent, stønadstype = stønadstype, journalpost = journalpost)
    }

    @Transactional
    fun håndterSøknad(
        journalpost: Journalpost,
        stønadstype: Stønadstype,
    ) {
        val personIdent = journalpostService.hentIdentFraJournalpost(journalpost)
        håndterSøknad(personIdent = personIdent, stønadstype = stønadstype, journalpost = journalpost)
    }

    private fun håndterSøknad(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        if (kanAutomatiskJournalføre(personIdent, stønadstype)) {
            journalføringService.journalførTilNyBehandling(
                journalpost = journalpost,
                personIdent = personIdent,
                stønadstype = stønadstype,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                oppgaveBeskrivelse = "Automatisk journalført søknad. Skal saksbehandles i ny løsning.",
                journalførendeEnhet = MASKINELL_JOURNALFOERENDE_ENHET,
            )
        } else {
            håndterSøknadSomIkkeKanAutomatiskJournalføres(
                personIdent = personIdent,
                stønadstype = stønadstype,
                journalpost = journalpost,
            )
        }
    }

    fun kanAutomatiskJournalføre(
        personIdent: String,
        stønadstype: Stønadstype,
    ): Boolean {
        val allePersonIdenter = personService.hentFolkeregisterIdenter(personIdent).identer()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, stønadstype)

        return if (fagsak == null) {
            true
        } else {
            val behandlinger = behandlingService.hentBehandlinger(fagsak.id)
            !harÅpenBehandling(behandlinger)
        }
    }

    private fun harÅpenBehandling(behandlinger: List<Behandling>): Boolean = behandlinger.any { !it.erAvsluttet() }

    private fun håndterSøknadSomIkkeKanAutomatiskJournalføres(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                personIdent = personIdent,
                stønadstype = stønadstype,
                oppgave =
                    OpprettOppgave(
                        oppgavetype = Oppgavetype.Journalføring,
                        enhetsnummer = journalpost.journalforendeEnhet?.takeIf { it != MASKINELL_JOURNALFOERENDE_ENHET },
                        beskrivelse = lagOppgavebeskrivelseForJournalføringsoppgave(journalpost),
                        journalpostId = journalpost.journalpostId,
                    ),
            ),
        )
    }

    private fun lagOppgavebeskrivelseForJournalføringsoppgave(journalpost: Journalpost): String {
        if (journalpost.dokumenter.isNullOrEmpty()) error("Journalpost ${journalpost.journalpostId} mangler dokumenter")
        val dokumentTittel = journalpost.dokumenter!!.firstOrNull { it.brevkode != null }?.tittel ?: ""
        return "Må behandles i ny løsning - $dokumentTittel"
    }
}
