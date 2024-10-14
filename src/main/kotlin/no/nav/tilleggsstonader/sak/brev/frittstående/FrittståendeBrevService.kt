package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.brev.BrevUtil
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereFrittståendeBrevService
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagringBrevService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FrittståendeBrevService(
    private val familieDokumentClient: FamilieDokumentClient,
    private val taskService: TaskService,
    private val mellomlagringBrevService: MellomlagringBrevService,
    private val frittståendeBrevRepository: FrittståendeBrevRepository,
    private val brevmottakereFrittståendeBrevService: BrevmottakereFrittståendeBrevService,
) {

    fun lagFrittståendeSanitybrev(
        request: GenererPdfRequest,
    ): ByteArray {
        val signatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)

        val htmlMedSignatur = BrevUtil.settInnSaksbehandlerSignaturOgDato(request.html, signatur)

        return familieDokumentClient.genererPdf(htmlMedSignatur)
    }

    @Transactional
    fun sendFrittståendeBrev(
        fagsakId: FagsakId,
        request: FrittståendeBrevDto,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()

        val frittståendeBrev = frittståendeBrevRepository.insert(
            FrittståendeBrev(
                fagsakId = fagsakId,
                pdf = Fil(request.pdf),
                tittel = request.tittel,
            ),
        )
        val brevmottakere = oppdaterBrevmottakereMedBrevId(frittståendeBrev)

        brevmottakere.forEach {
            taskService.save(JournalførFrittståendeBrevTask.opprettTask(fagsakId, frittståendeBrev.id, it.id))
        }

        mellomlagringBrevService.slettMellomlagretFrittståendeBrev(fagsakId, saksbehandler)
    }

    private fun oppdaterBrevmottakereMedBrevId(
        frittståendeBrev: FrittståendeBrev,
    ) = brevmottakereFrittståendeBrevService.hentEllerOpprettBrevmottakere(frittståendeBrev.fagsakId)
        .map { brevmottakereFrittståendeBrevService.oppdaterBrevmottaker(it.copy(brevId = frittståendeBrev.id)) }

    fun hentFrittståendeBrev(id: UUID): FrittståendeBrev {
        return frittståendeBrevRepository.findByIdOrThrow(id)
    }
}
