package no.nav.tilleggsstonader.sak.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
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
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.util.*

@Service
class JournalføringService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val journalpostService: JournalpostService,
    private val søknadService: SøknadService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val taskService: TaskService,
    private val barnService: BarnService,
) {
    fun journalførTilNyBehandling(
        journalpostId: String,
        personIdent: String,
        stønadstype: Stønadstype,
        oppgaveBeskrivelse: String,
    ) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, stønadstype)
        val nesteBehandlingstype = behandlingService.utledNesteBehandlingstype(fagsak.id)
        val journalførendeEnhet =
            arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(fagsak.hentAktivIdent())

        validerKanOppretteBehandling(personIdent, stønadstype, journalpost)

        val behandling = opprettBehandlingOgPopulerGrunnlagsdataForSøknad(
            behandlingstype = nesteBehandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
        )

        if (journalpost.harStrukturertSøknad()) {
            lagreSøknadOgBarn(journalpost, behandling)
        }

        ferdigstillJournalpost(journalpost, journalførendeEnhet, fagsak)

        opprettBehandleSakOppgaveTask(
            OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                behandlingId = behandling.id,
                saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                beskrivelse = oppgaveBeskrivelse,
            ),
        )
    }

    private fun ferdigstillJournalpost(
        journalpost: Journalpost,
        journalførendeEnhet: String,
        fagsak: Fagsak,
    ) {
        journalpostService.oppdaterOgFerdigstillJournalpostMaskinelt(
            journalpost = journalpost,
            journalførendeEnhet = journalførendeEnhet,
            fagsak = fagsak,
        )
    }

    private fun opprettBehandlingOgPopulerGrunnlagsdataForSøknad(
        behandlingstype: BehandlingType,
        fagsak: Fagsak,
        journalpost: Journalpost,
    ): Behandling {
        val behandling = behandlingService.opprettBehandling(
            behandlingType = behandlingstype,
            fagsakId = fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD,
        )

        behandlingService.leggTilBehandlingsjournalpost(journalpost.journalpostId, Journalposttype.I, behandling.id)

        /* TODO: Opprett statistikkinnslag */

        /* TODO: Opprett grunnlagsdata
          val grunnlagsdata = grunnlagsdataService.opprettGrunlagsdata(behandling.id)
         */
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

    private fun opprettBehandleSakOppgaveTask(opprettOppgaveTaskData: OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData) {
        taskService.save(OpprettOppgaveForOpprettetBehandlingTask.opprettTask(opprettOppgaveTaskData))
    }

    private fun validerKanOppretteBehandling(
        personIdent: String,
        stønadstype: Stønadstype,
        journalpost: Journalpost,
    ) {
        feilHvis(journalpost.bruker == null) {
            "Journalposten mangler bruker. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }

        feilHvis(journalpost.journalstatus != Journalstatus.MOTTATT) {
            "Journalposten har ugyldig journalstatus ${journalpost.journalstatus}. Kan ikke automatisk journalføre ${journalpost.journalpostId}"
        }
    }
}
