package no.nav.tilleggsstonader.sak.brev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.brev.brevmottaker.Brevmottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = JournalførVedtaksbrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Journalfører vedtaksbrev",
)
class JournalførVedtaksbrevTask(
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val brevmottakerRepository: BrevmottakerRepository,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val vedtaksbrev = brevService.hentBesluttetBrev(saksbehandling.id)
        val brevmottaker = hentBrevmottaker(saksbehandling)

        val dokument = Dokument(
            dokument = vedtaksbrev.beslutterPdf?.bytes ?: error("Mangler beslutterpdf"),
            filtype = Filtype.PDFA,
            dokumenttype = utledDokumenttype(saksbehandling),
            tittel = utledBrevtittel(saksbehandling),
        )

        val eksternReferanseId = "${saksbehandling.eksternId}-vedtaksbrev"

        val arkviverDokumentRequest = ArkiverDokumentRequest(
            fnr = saksbehandling.ident,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            fagsakId = saksbehandling.eksternFagsakId.toString(),
            journalførendeEnhet = arbeidsfordelingService.hentNavEnhet(saksbehandling.ident)?.enhetNr
                ?: error("Fant ikke arbeidsfordelingsenhet"),
            eksternReferanseId = eksternReferanseId,
            avsenderMottaker = lagAvsenderMottaker(brevmottaker),
        )

        try {
            val response = journalpostService.opprettJournalpost(arkviverDokumentRequest)

            feilHvisIkke(response.ferdigstilt) {
                "Journalposten ble ikke ferdigstilt og kan derfor ikke distribueres"
            }

            brevmottakerRepository.update(brevmottaker.copy(journalpostId = response.journalpostId))
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                logger.warn("Konflikt ved arkivering av dokument. Vedtaksbrevet har sannsynligvis allerede blitt arkivert for behandlingId=${saksbehandling.id} med eksternReferanseId=$eksternReferanseId")
            } else {
                throw e
            }
        }
    }

    private fun lagAvsenderMottaker(brevmottaker: Brevmottaker) = AvsenderMottaker(
        id = brevmottaker.ident,
        idType = when (brevmottaker.mottakerType) {
            MottakerType.PERSON -> BrukerIdType.FNR
            MottakerType.ORGANISASJON -> BrukerIdType.ORGNR
        },
        navn = when (brevmottaker.mottakerType) {
            MottakerType.PERSON -> null
            MottakerType.ORGANISASJON -> brevmottaker.navnHosOrganisasjon
        },
    )

    private fun hentBrevmottaker(saksbehandling: Saksbehandling): Brevmottaker {
        return brevmottakerRepository.findByBehandlingId(saksbehandling.id).let {
            feilHvis(it.size > 1) {
                "Støtte for flere brevmottakere er ikke implementert"
            }

            it.first()
        }
    }

    private fun utledBrevtittel(saksbehandling: Saksbehandling) = when (saksbehandling.stønadstype) {
        Stønadstype.BARNETILSYN -> "Vedtak om stønad til tilsyn barn" // TODO
        else -> error("Utledning av brevtype er ikke implementert for ${saksbehandling.stønadstype}")
    }

    private fun utledDokumenttype(saksbehandling: Saksbehandling) =
        when (saksbehandling.stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_VEDTAKSBREV
            else -> error("Utledning av dokumenttype er ikke implementert for ${saksbehandling.stønadstype}")
        }

    override fun onCompletion(task: Task) {
        taskService.save(DistribuerVedtaksbrevTask.opprettTask(UUID.fromString(task.payload)))
    }

    companion object {

        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties = Properties().apply {
                    setProperty("behandlingId", behandlingId.toString())
                },
            )

        const val TYPE = "journalførVedtaksbrev"
    }
}
