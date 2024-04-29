package no.nav.tilleggsstonader.sak.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Journalposttype
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.journalføring.dto.valider
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillJournalføringsoppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
open class JournalføringService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val journalpostService: JournalpostService,
    private val søknadService: SøknadService,
    private val taskService: TaskService,
    private val barnService: BarnService,
    private val transactionHandler: TransactionHandler,
    private val personService: PersonService,
    private val oppgaveService: OppgaveService,
) {

    @Transactional
    fun fullførJournalpost(
        journalføringRequest: JournalføringRequest,
        journalpost: Journalpost,
    ): String {
        journalføringRequest.valider()
        validerGyldigAvsender(journalpost, journalføringRequest)

        if (journalføringRequest.skalJournalføreTilNyBehandling()) {
            journalførTilNyBehandling(
                journalpostId = journalpost.journalpostId,
                personIdent = journalpostService.hentIdentFraJournalpost(journalpost),
                stønadstype = journalføringRequest.stønadstype,
                behandlingÅrsak = journalføringRequest.årsak.behandlingsårsak,
                journalførendeEnhet = journalføringRequest.journalførendeEnhet,
                dokumentTitler = journalføringRequest.dokumentTitler,
                logiskVedlegg = journalføringRequest.logiskeVedlegg,
            )
        } else {
            journalførUtenNyBehandling(journalføringRequest, journalpost)
        }

        ferdigstillJournalføringsoppgave(journalføringRequest.oppgaveId)

        return journalpost.journalpostId
    }

    private fun ferdigstillJournalføringsoppgave(oppgaveId: String) {
        try {
            oppgaveService.ferdigstillOppgave(oppgaveId.toLong())
        } catch (e: Exception) {
            logger.warn("Kunne ikke ferdigstille journalføringsoppgave=$oppgaveId. Oppretter task for ferdigstillelse")
            opprettFerdigstillJournalføringsoppgaveTask(oppgaveId)
        }
    }

    private fun opprettFerdigstillJournalføringsoppgaveTask(oppgaveId: String) {
        taskService.save(FerdigstillJournalføringsoppgaveTask.opprettTask(oppgaveId))
    }

    fun journalførTilNyBehandling(
        journalpostId: String,
        personIdent: String,
        stønadstype: Stønadstype,
        behandlingÅrsak: BehandlingÅrsak,
        oppgaveBeskrivelse: String? = null,
        journalførendeEnhet: String,
        dokumentTitler: Map<String, String>? = null,
        logiskVedlegg: Map<String, List<LogiskVedlegg>>? = null,
    ) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val fagsak = hentEllerOpprettFagsakIEgenTransaksjon(personIdent, stønadstype)
        val nesteBehandlingstype = behandlingService.utledNesteBehandlingstype(fagsak.id)

        validerKanOppretteBehandling(journalpost, personIdent)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdataForJournalpost(
            behandlingstype = nesteBehandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
            behandlingÅrsak = behandlingÅrsak,
        )

        if (journalpost.harStrukturertSøknad()) {
            lagreSøknadOgBarn(journalpost, behandling)
        }

        ferdigstillJournalpost(journalpost, journalførendeEnhet, fagsak, dokumentTitler, logiskVedlegg)

        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                    beskrivelse = oppgaveBeskrivelse,
                ),
            ),
        )
    }

    private fun journalførUtenNyBehandling(journalføringRequest: JournalføringRequest, journalpost: Journalpost) {
        val fagsak =
            hentEllerOpprettFagsakIEgenTransaksjon(
                journalpostService.hentIdentFraJournalpost(journalpost),
                journalføringRequest.stønadstype,
            )
        ferdigstillJournalpost(
            journalpost,
            journalføringRequest.journalførendeEnhet,
            fagsak,
            journalføringRequest.dokumentTitler,
            journalføringRequest.logiskeVedlegg,
        )
    }

    private fun hentEllerOpprettFagsakIEgenTransaksjon(
        personIdent: String,
        stønadstype: Stønadstype,
    ) = transactionHandler.runInNewTransaction {
        fagsakService.hentEllerOpprettFagsak(personIdent, stønadstype)
    }

    private fun ferdigstillJournalpost(
        journalpost: Journalpost,
        journalførendeEnhet: String,
        fagsak: Fagsak,
        dokumentTitler: Map<String, String>? = null,
        logiskVedlegg: Map<String, List<LogiskVedlegg>>? = null,
    ) {
        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            journalførendeEnhet = journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            dokumenttitler = dokumentTitler,
            logiskeVedlegg = logiskVedlegg,
        )
    }

    private fun opprettBehandlingOgPopulerGrunnlagsdataForJournalpost(
        behandlingstype: BehandlingType,
        fagsak: Fagsak,
        journalpost: Journalpost,
        behandlingÅrsak: BehandlingÅrsak,
    ): Behandling {
        val behandling = behandlingService.opprettBehandling(
            behandlingType = behandlingstype,
            fagsakId = fagsak.id,
            behandlingsårsak = behandlingÅrsak,
        )

        behandlingService.leggTilBehandlingsjournalpost(journalpost.journalpostId, Journalposttype.I, behandling.id)

        return behandling
    }

    private fun lagreSøknadOgBarn(
        journalpost: Journalpost,
        behandling: Behandling,
    ) {
        lagreSøknad(journalpost, behandling.id)
        val barn = søknadService.hentSøknadBarnetilsyn(behandling.id)?.barn?.map {
            BehandlingBarn(
                behandlingId = behandling.id,
                ident = it.ident,
                søknadBarnId = it.id,
            )
        } ?: error("Søknad mangler barn")

        barnService.opprettBarn(barn)
    }

    private fun lagreSøknad(journalpost: Journalpost, behandlingId: UUID) {
        val søknad = journalpostService.hentSøknadFraJournalpost(journalpost)
        søknadService.lagreSøknad(behandlingId, journalpost, søknad)
    }

    private fun validerKanOppretteBehandling(
        journalpost: Journalpost,
        personIdent: String,
    ) {
        feilHvis(journalpost.bruker == null) {
            "Journalposten mangler bruker. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }

        feilHvis(journalpost.journalstatus != Journalstatus.MOTTATT) {
            "Journalposten har ugyldig journalstatus ${journalpost.journalstatus}. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }

        journalpost.bruker?.let {
            val allePersonIdenter = personService.hentPersonIdenter(personIdent).identer()
            feilHvisIkke(fagsakPersonOgJournalpostBrukerErSammePerson(allePersonIdenter, personIdent, it)) {
                "Ikke samsvar mellom personident på journalposten og personen vi forsøker å opprette behandling for. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
            }
        }
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

    private fun validerGyldigAvsender(journalpost: Journalpost, request: JournalføringRequest) {
        if (journalpost.manglerAvsenderMottaker()) {
            brukerfeilHvis(request.nyAvsender == null) {
                "Kan ikke journalføre uten avsender"
            }
            brukerfeilHvis(!request.nyAvsender.erBruker && request.nyAvsender.navn.isNullOrBlank()) {
                "Må sende inn navn på ny avsender"
            }
            brukerfeilHvis(request.nyAvsender.erBruker && request.nyAvsender.personIdent.isNullOrBlank()) {
                "Må sende inn ident på ny avsender hvis det er bruker"
            }
        } else {
            brukerfeilHvis(request.nyAvsender != null) {
                "Kan ikke endre avsender på journalpost som har avsender fra før"
            }
        }
    }
}
