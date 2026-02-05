package no.nav.tilleggsstonader.sak.hendelser.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterMottattKjørelisteService
import no.nav.tilleggsstonader.sak.ekstern.journalføring.HåndterSøknadService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.brevkoder
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import no.nav.tilleggsstonader.sak.journalføring.erInnkommende
import no.nav.tilleggsstonader.sak.journalføring.gjelderKanalSkanningEllerNavNo
import no.nav.tilleggsstonader.sak.journalføring.gjelderKjøreliste
import no.nav.tilleggsstonader.sak.journalføring.gjelderSøknad
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Håndterer journalhendelser fra dokarkiv.
 * Alle nye tilleggsstønader-søkander prosesseres herfra
 */
@Service
class JournalhendelseKafkaHåndtererService(
    private val journalpostService: JournalpostService,
    private val håndterSøknadService: HåndterSøknadService,
    private val journalpostMottattMetrikker: JournalpostMottattMetrikker,
    private val håndterMottattKjørelisteService: HåndterMottattKjørelisteService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun behandleJournalhendelse(journalpostId: String) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)

        if (journalpost.kanBehandles()) {
            logSkalBehandles(journalpost, kanBehandles = true)
            behandleMottattJournalpost(journalpost)
        } else if (journalpost.erInnkommende()) {
            logSkalBehandles(journalpost, kanBehandles = false)
        }
        // Trenger ikke å logge andre journalposter av typen utgående eller notat

        // Teller antall mottatte journalposter per brevkode. Ignorerer evt ferdigstilte
        if (!journalpost.erFerdigstilt()) {
            journalpostMottattMetrikker.journalpostMottatt(journalpost)
        }
    }

    private fun behandleMottattJournalpost(journalpost: Journalpost) {
        if (journalpost.gjelderSøknad()) {
            håndterSøknadService.håndterSøknad(journalpost)
        } else if (journalpost.gjelderKjøreliste()) {
            håndterMottattKjørelisteService.behandleKjøreliste(journalpost)
        } else {
            error("Kan ikke behandle journalpost med brevkode ${journalpost.dokumentBrevkode()}")
        }
    }

    private fun Journalpost.kanBehandles() =
        Tema.gjelderTemaTilleggsstønader(this.tema) &&
            this.erInnkommende() &&
            this.gjelderKanalSkanningEllerNavNo() &&
            (this.gjelderSøknad() || this.gjelderKjøreliste()) &&
            !this.erFerdigstilt()

    /*
     Kan komme inn ferdigstilte journalposter om man har endret sakstilknytning på et allerede journalført dokument.
     Kommer i så fall to hendelser "JournalpostMottatt" og deretter "EndeligJournalført" rett etter hverandre,
     dette fordi man ikke kan endre en ferdigstilt journalpost og man da kopierer den originale som en ny en
     */
    private fun Journalpost.erFerdigstilt() =
        this.journalstatus == Journalstatus.FERDIGSTILT || this.journalstatus == Journalstatus.JOURNALFOERT

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
                "brevkode=${journalpost.brevkoder()} " +
                "relevanteDatoer=${journalpost.relevanteDatoer}",
        )
    }
}
