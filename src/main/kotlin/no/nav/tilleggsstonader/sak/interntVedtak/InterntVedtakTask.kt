package no.nav.tilleggsstonader.sak.interntVedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.dokumentTypeInterntVedtak
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = InterntVedtakTask.TYPE,
    beskrivelse = "Oppretter og journalfører internt vedtak",
    maxAntallFeil = 3,
)
class InterntVedtakTask(
    private val interntVedtakService: InterntVedtakService,
    private val htmlifyClient: HtmlifyClient,
    private val dokumentClient: FamilieDokumentClient,
    private val journalpostService: JournalpostService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val interntVedtak = interntVedtakService.lagInterntVedtak(behandlingId)
        val html = htmlifyClient.generateHtml(interntVedtak)
        val pdf = dokumentClient.genererPdf(html)
        arkiver(interntVedtak.behandling, pdf)
    }

    private fun arkiver(
        behandlingInfo: Behandlinginfo,
        pdf: ByteArray,
    ) {
        val behandlingId = behandlingInfo.behandlingId
        val stønadstype = behandlingInfo.stønadstype
        val enhet =
            arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(behandlingInfo.ident)
        journalpostService.opprettJournalpost(
            ArkiverDokumentRequest(
                fnr = behandlingInfo.ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter =
                    listOf(
                        Dokument(
                            dokument = pdf,
                            filtype = Filtype.PDFA,
                            filnavn = null,
                            tittel = "Internt vedtak $stønadstype",
                            dokumenttype = stønadstype.dokumentTypeInterntVedtak(),
                        ),
                    ),
                fagsakId = behandlingInfo.eksternFagsakId.toString(),
                avsenderMottaker = null,
                journalførendeEnhet = enhet,
                eksternReferanseId = "$behandlingId-blankett",
            ),
        )
    }

    companion object {
        const val TYPE = "lagInterntVedtak"

        fun lagTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
            )
    }
}
