package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.HåndterSøknadRequest
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.journalføring.JournalføringService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskJournalføringService(
    private val personService: PersonService,
    private val journalpostService: JournalpostService,
    private val taskService: TaskService,
    private val journalføringService: JournalføringService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val unleashService: UnleashService,
) {

    @Transactional
    fun håndterSøknad(request: HåndterSøknadRequest) {
        val personIdent = request.personIdent
        val stønadstype = request.stønadstype

        if (kanAutomatiskJournalføre(personIdent, stønadstype)) {
            journalføringService.journalførTilNyBehandling(
                journalpostId = request.journalpostId,
                personIdent = personIdent,
                stønadstype = stønadstype,
                oppgaveBeskrivelse = "Automatisk journalført søknad",
            )
        } else {
            håndterSøknadSomIkkeKanAutomatiskJournalføres(request)
        }
    }

    fun kanAutomatiskJournalføre(personIdent: String, stønadstype: Stønadstype): Boolean {
        val allePersonIdenter = personService.hentPersonIdenter(personIdent).identer()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, stønadstype)

        return if (fagsak == null) {
            true
        } else {
            val nesteBehandlingstype = behandlingService.utledNesteBehandlingstype(fagsak.id)

            if (nesteBehandlingstype === BehandlingType.REVURDERING && !unleashService.isEnabled(Toggle.AUTOMATISK_JOURNALFORING_REVURDERING)) {
                return false
            }

            val behandlinger = behandlingService.hentBehandlinger(fagsak.id)
            !harÅpenBehandling(behandlinger)
        }
    }

    private fun harÅpenBehandling(behandlinger: List<Behandling>): Boolean {
        return behandlinger.any { !it.erAvsluttet() }
    }

    private fun håndterSøknadSomIkkeKanAutomatiskJournalføres(request: HåndterSøknadRequest) {
        val journalpost = journalpostService.hentJournalpost(request.journalpostId)
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                personIdent = request.personIdent,
                stønadstype = request.stønadstype,
                oppgave = OpprettOppgave(
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

    private fun fagsakPersonOgJournalpostBrukerErSammePerson(
        allePersonIdenter: Set<String>,
        gjeldendePersonIdent: String,
        journalpostBruker: Bruker,
    ): Boolean = when (journalpostBruker.type) {
        BrukerIdType.FNR -> allePersonIdenter.contains(journalpostBruker.id)
        BrukerIdType.AKTOERID -> hentAktørIderForPerson(gjeldendePersonIdent).contains(journalpostBruker.id)
        BrukerIdType.ORGNR -> false
    }

    private fun hentAktørIderForPerson(personIdent: String) =
        personService.hentAktørIder(personIdent).identer()
}
