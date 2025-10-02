package no.nav.tilleggsstonader.sak.hendelser.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode.BARNETILSYN
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode.BOUTGIFTER
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode.DAGLIG_REISE
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode.LÆREMIDLER
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.brevkoder
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import no.nav.tilleggsstonader.sak.journalføring.erInnkommende
import no.nav.tilleggsstonader.sak.journalføring.gjelderKanalSkanningEllerNavNo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Håndterer journalhendelser fra dokarkiv.
 * Foreløpig håndterer vi kun innkommende journalposter med brevkode for boutgifter.
 */
@Service
class JournalhendelseKafkaHåndtererService(
    private val journalpostService: JournalpostService,
    private val håndterSøknadService: HåndterSøknadService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun behandleJournalhendelse(journalpostId: String) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)

        if (journalpost.kanBehandles()) {
            logSkalBehandles(journalpost, kanBehandles = true)
            håndterSøknadService.håndterSøknad(journalpost)
        } else if (journalpost.erInnkommende()) {
            logSkalBehandles(journalpost, kanBehandles = false)
        }
        // Trenger ikke å logge andre journalposter av typen utgående eller notat
    }

    private fun Journalpost.kanBehandles() =
        Tema.gjelderTemaTilleggsstønader(this.tema) &&
            this.erInnkommende() &&
            this.gjelderKanalSkanningEllerNavNo() &&
            this.dokumentBrevkode() in listOf(BARNETILSYN, LÆREMIDLER, BOUTGIFTER, DAGLIG_REISE)

    private fun logSkalBehandles(
        journalpost: Journalpost,
        kanBehandles: Boolean,
    ) {
        logger.info(
            "BehandleJournalpost " +
                "kanBehandles=$kanBehandles " +
                "journalpost=${journalpost.journalpostId} " +
                "tema=${journalpost.tema} " +
                "status=${journalpost.journalstatus} " +
                "type=${journalpost.journalposttype} " +
                "kanal=${journalpost.kanal} " +
                "brevkode=${journalpost.brevkoder()}",
        )
    }
}
