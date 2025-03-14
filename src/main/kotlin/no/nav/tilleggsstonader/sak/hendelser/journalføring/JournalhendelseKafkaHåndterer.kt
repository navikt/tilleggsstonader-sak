package no.nav.tilleggsstonader.sak.hendelser.journalføring

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.brevkoder
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import no.nav.tilleggsstonader.sak.journalføring.erInnkommende
import no.nav.tilleggsstonader.sak.journalføring.gjelderKanalSkanningEllerNavNo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalhendelseKafkaHåndterer(
    private val journalpostService: JournalpostService,
    private val håndterSøknadService: HåndterSøknadService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun behandleJournalpost(hendelseRecord: JournalfoeringHendelseRecord) {
        val journalpost = journalpostService.hentJournalpost(hendelseRecord.journalpostId.toString())
        val kanBehandles = journalpost.kanBehandles()

        val brevkode = journalpost.dokumentBrevkode()
        if (kanBehandles && brevkode == DokumentBrevkode.BOUTGIFTER) {
            val stønadstype = brevkode.stønadstype
            logSkalBehandles(journalpost, kanBehandles = true)
            håndterSøknadService.håndterSøknad(journalpost, stønadstype)
        } else if (journalpost.erInnkommende()) {
            logSkalBehandles(journalpost, kanBehandles = false)
        }
        // Trenger ikke å logge andre journalposter av typen utgående eller notat
    }

    private fun Journalpost.kanBehandles() =
        Tema.gjelderTemaTilleggsstønader(this.tema) &&
            this.erInnkommende() &&
            this.gjelderKanalSkanningEllerNavNo()

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
