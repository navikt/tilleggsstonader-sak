package no.nav.tilleggsstonader.sak.journalføring

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.AutomatiskJournalføringResponse
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JournalføringService(
    private val journalpostService: JournalpostService,
    private val behandlingService: BehandlingService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val taskService: TaskService
) {

    @Transactional
    fun automatiskJournalfør(
        fagsak: Fagsak,
        journalpost: Journalpost,
        journalførendeEnhet: String,
        mappeId: Long?,
        behandlingstype: BehandlingType,
        prioritet: OppgavePrioritet,
    ): AutomatiskJournalføringResponse {
        val behandling = opprettBehandlingOgPopulerGrunnlagsdataForAutomatiskJournalførtSøknad(
            behandlingstype = behandlingstype,
            fagsak = fagsak,
            journalpost = journalpost,
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
                beskrivelse = AUTOMATISK_JOURNALFØRING_BESKRIVELSE,
                mappeId = mappeId,
                prioritet = prioritet,
            ),
        )
        return AutomatiskJournalføringResponse(
            fagsakId = fagsak.id,
            behandlingId = behandling.id,
        )
    }

    private fun opprettBehandlingOgPopulerGrunnlagsdataForAutomatiskJournalførtSøknad(
        behandlingstype: BehandlingType,
        fagsak: Fagsak,
        journalpost: Journalpost,
//        ustrukturertDokumentasjonType: UstrukturertDokumentasjonType = UstrukturertDokumentasjonType.IKKE_VALGT,
    ): Behandling {
        val behandling = behandlingService.opprettBehandling(
            behandlingType = behandlingstype,
            fagsakId = fagsak.id,
            behandlingsårsak = BehandlingÅrsak.SØKNAD,
        )
        // iverksettService.startBehandling(behandling, fagsak)

        // settSøknadPåBehandling(journalpost, fagsak, behandling.id)
//        knyttJournalpostTilBehandling(journalpost, behandling)

        val grunnlagsdata = grunnlagsdataService.opprettGrunlagsdata(behandling.id)
//        barnService.opprettBarnPåBehandlingMedSøknadsdata(
//            behandlingId = behandling.id,
//            fagsakId = fagsak.id,
//            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
//            stønadstype = fagsak.stønadstype,
//            ustrukturertDokumentasjonType = ustrukturertDokumentasjonType,
//            barnSomSkalFødes = barnSomSkalFødes,
//            vilkårsbehandleNyeBarn = vilkårsbehandleNyeBarn,
//        )

        return behandling
    }

    private fun opprettBehandleSakOppgaveTask(opprettOppgaveTaskData: OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData) {
        taskService.save(OpprettOppgaveForOpprettetBehandlingTask.opprettTask(opprettOppgaveTaskData))
    }

    companion object {

        const val AUTOMATISK_JOURNALFØRING_BESKRIVELSE = "Automatisk journalført"
    }
}