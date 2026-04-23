package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.dokumenttyper
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.journalføring.ArkiverDokumentConflictException
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførKjørelisteBehandlingBrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Journalfører kjørelistebrev",
)
class JournalførKjørelisteBehandlingBrevTask(
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val kjørelisteBehandlingBrevService: KjørelisteBehandlingBrevService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val stegService: StegService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val brev = kjørelisteBehandlingBrevService.hentLagretBrev(behandlingId)

        val dokumenttype =
            saksbehandling.stønadstype.dokumenttyper.kjøreliste
                ?: error("Stønadstype ${saksbehandling.stønadstype} mangler kjøreliste-dokumenttype")

        val dokument =
            Dokument(
                dokument = brev.pdf.bytes,
                filtype = Filtype.PDFA,
                dokumenttype = dokumenttype,
                tittel = "Brev om kjørelisteutbetaling",
            )

        val eksternReferanseId = "${saksbehandling.eksternId}-kjørelistebrev"
        val arkiverDokumentRequest =
            ArkiverDokumentRequest(
                fnr = saksbehandling.ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(dokument),
                fagsakId = saksbehandling.eksternFagsakId.toString(),
                journalførendeEnhet =
                    arbeidsfordelingService.hentNavEnhet(saksbehandling.ident, saksbehandling.stønadstype)?.enhetNr
                        ?: error("Fant ikke arbeidsfordelingsenhet"),
                eksternReferanseId = eksternReferanseId,
            )

        val arkiverDokumentResponse = opprettJournalpost(arkiverDokumentRequest, behandlingId, eksternReferanseId)

//        kjørelisteBehandlingBrevService.lagreJournalpostId(behandlingId, arkiverDokumentResponse.journalpostId)
        stegService.håndterSteg(behandlingId, StegType.JOURNALFØR_OG_DISTRIBUER_KJØRELISTEBREV)
    }

    private fun opprettJournalpost(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        behandlingId: BehandlingId,
        eksternReferanseId: String,
    ): ArkiverDokumentResponse {
        val response =
            try {
                journalpostService.opprettJournalpost(arkiverDokumentRequest)
            } catch (e: ArkiverDokumentConflictException) {
                logger.warn(
                    "Konflikt ved arkivering av dokument. Kjørelistebrevet har sannsynligvis allerede blitt arkivert" +
                        " for behandlingId=$behandlingId med eksternReferanseId=$eksternReferanseId",
                )
                e.response
            }
        feilHvisIkke(response.ferdigstilt) {
            "Journalposten ble ikke ferdigstilt og kan derfor ikke distribueres"
        }
        return response
    }

    override fun onCompletion(task: Task) {
        taskService.save(DistribuerKjørelisteBehandlingBrevTask.opprettTask(BehandlingId.fromString(task.payload)))
    }

    companion object {
        fun opprettTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        setProperty("behandlingId", behandlingId.toString())
                    },
            )

        const val TYPE = "journalførKjørelistebrev"
    }
}
