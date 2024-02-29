package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.HåndterSøknadRequest
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.Journalposttype
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.OpprettOppgaveTask
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AutomatiskJournalføringService(
    private val fagsakService: FagsakService,
    private val personService: PersonService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val taskService: TaskService,
    private val barnService: BarnService,
    private val grunnlagsdataService: GrunnlagsdataService,
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun håndterSøknad(request: HåndterSøknadRequest) {
        val personIdent = request.personIdent
        val stønadstype = request.stønadstype

        if (kanOppretteBehandling(personIdent, stønadstype)) {
            automatiskJournalførTilBehandling(
                journalpostId = request.journalpostId,
                personIdent = personIdent,
                stønadstype = stønadstype,
            )
        } else {
            håndterSøknadSomIkkeKanAutomatiskJournalføres(request)
        }
    }

    private fun automatiskJournalførTilBehandling(
        journalpostId: String,
        personIdent: String,
        stønadstype: Stønadstype,
    ) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, stønadstype)
        val nesteBehandlingstype = utledNesteBehandlingstype(behandlingService.hentBehandlinger(fagsak.id))
        val journalførendeEnhet =
            arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(fagsak.hentAktivIdent())

        validerKanAutomatiskJournalføre(personIdent, stønadstype, journalpost)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdataForAutomatiskJournalførtSøknad(
            behandlingstype = nesteBehandlingstype,
            fagsak = fagsak,
            søknad = journalpost,
        )

        journalpostService.oppdaterOgFerdigstillJournalpostMaskinelt(
            journalpost = journalpost,
            journalførendeEnhet = journalførendeEnhet,
            fagsak = fagsak,
        )

        opprettBehandleSakOppgaveTask(
            OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                behandlingId = behandling.id,
                saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                beskrivelse = "Automatisk journalført søknad",
            ),
        )
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

    fun kanOppretteBehandling(ident: String, stønadstype: Stønadstype): Boolean {
        val allePersonIdenter = personService.hentPersonIdenter(ident).identer()
        val fagsak = fagsakService.finnFagsak(allePersonIdenter, stønadstype)
        val behandlinger = fagsak?.let { behandlingService.hentBehandlinger(fagsak.id) } ?: emptyList()

        return if (!harÅpenBehandling(behandlinger)) {
            secureLogger.info("Kan automatisk journalføre for fagsak: ${fagsak?.id}")
            true
        } else {
            secureLogger.info("Kan ikke automatisk journalføre for fagsak: ${fagsak?.id}")
            false
        }
    }

    private fun validerKanAutomatiskJournalføre(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        val allePersonIdenter = personService.hentPersonIdenter(personIdent).identer()

        feilHvisIkke(kanOppretteBehandling(personIdent, stønadstype)) {
            "Kan ikke opprette førstegangsbehandling for $stønadstype da det allerede finnes en behandling i infotrygd eller ny løsning"
        }

        feilHvis(journalpost.bruker == null) {
            "Journalposten mangler bruker. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }

        feilHvis(journalpost.journalstatus != Journalstatus.MOTTATT) {
            "Journalposten har ugyldig journalstatus ${journalpost.journalstatus}. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
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

    private fun utledNesteBehandlingstype(behandlinger: List<Behandling>): BehandlingType {
        return if (behandlinger.all { it.resultat == BehandlingResultat.HENLAGT }) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING
    }

    private fun harÅpenBehandling(behandlinger: List<Behandling>): Boolean {
        return behandlinger.any { !it.erAvsluttet() }
    }

    private fun opprettBehandlingOgPopulerGrunnlagsdataForAutomatiskJournalførtSøknad(
        behandlingstype: BehandlingType,
        fagsak: Fagsak,
        søknad: Journalpost,
    ): Behandling {
        val behandling = behandlingService.opprettBehandling(
            behandlingType = behandlingstype,
            fagsakId = fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD,
        )

        lagreSøknad(søknad, fagsak, behandling.id)
        behandlingService.leggTilBehandlingsjournalpost(søknad.journalpostId, Journalposttype.I, behandling.id)

        /* TODO: Opprett statistikkinnslag */

        /* TODO: Opprett grunnlagsdata
          val grunnlagsdata = grunnlagsdataService.opprettGrunlagsdata(behandling.id)
         */

        val barn = søknadService.hentSøknadBarnetilsyn(behandling.id)?.barn?.map {
            BehandlingBarn(
                behandlingId = behandling.id,
                ident = it.ident,
                søknadBarnId = it.id,
            )
        } ?: error("Søknad mangler barn")

        barnService.opprettBarn(barn)

        return behandling
    }

    private fun lagreSøknad(søknadJournalpost: Journalpost, fagsak: Fagsak, behandlingId: UUID) {
        val søknad = journalpostService.hentSøknadFraJournalpost(søknadJournalpost)
        søknadService.lagreSøknad(behandlingId, søknadJournalpost.journalpostId, søknad)
    }

    private fun opprettBehandleSakOppgaveTask(opprettOppgaveTaskData: OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData) {
        taskService.save(OpprettOppgaveForOpprettetBehandlingTask.opprettTask(opprettOppgaveTaskData))
    }
}
