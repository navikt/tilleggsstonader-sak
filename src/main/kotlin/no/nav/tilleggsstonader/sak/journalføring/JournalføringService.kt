package no.nav.tilleggsstonader.sak.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.GjennbrukDataRevurderingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Journalposttype
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.gjelderBarn
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.JournalføringHelper.tilAvsenderMottaker
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.journalføring.dto.valider
import no.nav.tilleggsstonader.sak.klage.KlageService
import no.nav.tilleggsstonader.sak.klage.dto.OpprettKlageDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillJournalføringsoppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JournalføringService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val journalpostService: JournalpostService,
    private val søknadService: SøknadService,
    private val taskService: TaskService,
    private val barnService: BarnService,
    private val transactionHandler: TransactionHandler,
    private val personService: PersonService,
    private val oppgaveService: OppgaveService,
    private val gjennbrukDataRevurderingService: GjennbrukDataRevurderingService,
    private val klageService: KlageService,
) {

    @Transactional
    fun fullførJournalpost(
        journalføringRequest: JournalføringRequest,
        journalpost: Journalpost,
    ): String {
        journalføringRequest.valider()
        validerGyldigAvsender(journalpost, journalføringRequest)

        if (journalføringRequest.skalJournalføreTilNyBehandling() && !journalføringRequest.gjelderKlage()) {
            journalførTilNyBehandling(
                journalpostId = journalpost.journalpostId,
                personIdent = journalpostService.hentIdentFraJournalpost(journalpost),
                stønadstype = journalføringRequest.stønadstype,
                behandlingÅrsak = journalføringRequest.årsak.behandlingsårsak,
                journalførendeEnhet = journalføringRequest.journalførendeEnhet,
                dokumentTitler = journalføringRequest.dokumentTitler,
                logiskVedlegg = journalføringRequest.logiskeVedlegg,
            )
        } else if (journalføringRequest.skalJournalføreTilNyBehandling() && journalføringRequest.gjelderKlage()) {
            journalførTilNyKlage(
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

        validerKanOppretteBehandling(journalpost, fagsak, behandlingÅrsak, gjelderKlage = false)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdataForJournalpost(
            fagsak = fagsak,
            journalpost = journalpost,
            behandlingÅrsak = behandlingÅrsak,
        )

        val behandlingIdForGjenbruk = gjennbrukDataRevurderingService.finnBehandlingIdForGjenbruk(behandling)

        if (behandlingIdForGjenbruk != null) {
            gjennbrukDataRevurderingService.gjenbrukData(behandling, behandlingIdForGjenbruk)
        }

        if (journalpost.harStrukturertSøknad()) {
            lagreSøknadOgNyeBarn(journalpost, behandling, stønadstype)
        }

        ferdigstillJournalpost(journalpost, journalførendeEnhet, fagsak, dokumentTitler, logiskVedlegg)

        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = behandling.id,
                    saksbehandler = null, // Behandle sak oppgaven skal være ufordelt
                    beskrivelse = oppgaveBeskrivelse,
                ),
            ),
        )
    }

    fun journalførTilNyKlage(
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

        validerKanOppretteBehandling(journalpost, fagsak, behandlingÅrsak, gjelderKlage = true)

        feilHvis(journalpost.harStrukturertSøknad()) { "Journalpost med id=${journalpost.journalpostId} gjelder ikke en Klagebehandling." }

        klageService.opprettKlage(fagsakId = fagsak.id, OpprettKlageDto(journalpost.datoMottatt?.toLocalDate() ?: osloDateNow()))

        ferdigstillJournalpost(journalpost, journalførendeEnhet, fagsak, dokumentTitler, logiskVedlegg)
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
            journalføringRequest.nyAvsender?.tilAvsenderMottaker(),
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
        avsenderMottaker: AvsenderMottaker? = null,
    ) {
        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            journalførendeEnhet = journalførendeEnhet,
            fagsak = fagsak,
            saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            dokumenttitler = dokumentTitler,
            logiskeVedlegg = logiskVedlegg,
            avsender = avsenderMottaker,
        )
    }

    private fun opprettBehandlingOgPopulerGrunnlagsdataForJournalpost(
        fagsak: Fagsak,
        journalpost: Journalpost,
        behandlingÅrsak: BehandlingÅrsak,
    ): Behandling {
        val behandling = behandlingService.opprettBehandling(
            fagsakId = fagsak.id,
            behandlingsårsak = behandlingÅrsak,
        )

        behandlingService.leggTilBehandlingsjournalpost(journalpost.journalpostId, Journalposttype.I, behandling.id)

        return behandling
    }

    private fun lagreSøknadOgNyeBarn(
        journalpost: Journalpost,
        behandling: Behandling,
        stønadstype: Stønadstype,
    ) {
        lagreSøknad(journalpost, behandling.id, stønadstype)
        if (stønadstype.gjelderBarn()) {
            lagreBarn(behandling)
        }
    }

    private fun lagreBarn(behandling: Behandling) {
        val eksisterendeBarn = barnService.finnBarnPåBehandling(behandling.id)

        val nyeBarn = søknadService.hentSøknadBarnetilsyn(behandling.id)?.barn
            ?.filterNot { barn -> eksisterendeBarn.any { it.ident == barn.ident } }?.map {
                BehandlingBarn(
                    behandlingId = behandling.id,
                    ident = it.ident,
                )
            } ?: emptyList()

        feilHvis(nyeBarn.isEmpty() && eksisterendeBarn.isEmpty()) {
            "Kan ikke opprette behandling uten barn"
        }

        barnService.opprettBarn(nyeBarn)
    }

    private fun lagreSøknad(journalpost: Journalpost, behandlingId: BehandlingId, stønadstype: Stønadstype) {
        val søknad = journalpostService.hentSøknadFraJournalpost(journalpost, stønadstype)
        søknadService.lagreSøknad(behandlingId, journalpost, søknad)
    }

    private fun validerKanOppretteBehandling(
        journalpost: Journalpost,
        fagsak: Fagsak,
        behandlingÅrsak: BehandlingÅrsak,
        gjelderKlage: Boolean,
    ) {
        val personIdent = fagsak.hentAktivIdent()
        feilHvis(journalpost.bruker == null) {
            "Journalposten mangler bruker. Kan ikke journalføre ${journalpost.journalpostId}"
        }

        feilHvis(journalpost.journalstatus != Journalstatus.MOTTATT) {
            "Journalposten har ugyldig journalstatus ${journalpost.journalstatus}. Kan ikke journalføre ${journalpost.journalpostId}"
        }

        journalpost.bruker?.let {
            val allePersonIdenter = personService.hentPersonIdenter(personIdent).identer()
            feilHvisIkke(fagsakPersonOgJournalpostBrukerErSammePerson(allePersonIdenter, personIdent, it)) {
                "Ikke samsvar mellom personident på journalposten og personen vi forsøker å opprette behandling for. Kan ikke journalføre ${journalpost.journalpostId}"
            }
        }

        if (fagsak.stønadstype.gjelderBarn() && !gjelderKlage) {
            validerKanOppretteNyBehandlingSomKanInneholdeNyeBarn(behandlingÅrsak, fagsak)
        }
    }

    private fun skalValidereForAtManMåOppretteNyBehandlingManueltPgaNyeBarn(stønadstype: Stønadstype): Boolean {
        return when (stønadstype) {
            Stønadstype.BARNETILSYN -> true
            Stønadstype.LÆREMIDLER -> false
            else -> error("Har ikke tatt stilling til om $stønadstype skal validere for nye barn")
        }
    }

    /**
     * For tilsyn barn må man ta stilling til hvilke barn som skal legges til i behandlingen.
     * Vi tror behovet for dette er begrenset og er allerede støttet på annen måte.
     * Ønsker derfor at disse journalføringene gjøres uten å opprette behandling, men at behandlingen opprettes manuelt
     */
    private fun validerKanOppretteNyBehandlingSomKanInneholdeNyeBarn(
        behandlingÅrsak: BehandlingÅrsak,
        fagsak: Fagsak,
    ) {
        feilHvis(behandlingÅrsak == BehandlingÅrsak.PAPIRSØKNAD) {
            feilmeldingOpprettBehandlingManuelt("papirsøknad")
        }
        feilHvis(
            behandlingÅrsak == BehandlingÅrsak.NYE_OPPLYSNINGER &&
                !behandlingService.finnesBehandlingForFagsak(fagsak.id),
        ) {
            feilmeldingOpprettBehandlingManuelt("ettersending")
        }
    }

    private fun feilmeldingOpprettBehandlingManuelt(typeJournalføring: String) =
        "Journalføring av $typeJournalføring må gjøres uten å opprette ny behandling. " +
            "Behandlingen må opprettes manuelt etter journalføringen. " +
            "Hvis søker har behandling fra før så kan det gjøres fra behandlingsoversikten. " +
            "Hvis søker ikke har behandling fra før så må det gjøres via Admin - opprett behandling"

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
