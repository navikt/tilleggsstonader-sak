package no.nav.tilleggsstonader.sak.ekstern.`journalføring`

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.springframework.stereotype.Service

@Service
class HåndterMottattKjørelisteService(private val journalpostService: JournalpostService) {

    fun behandleKjøreliste(journalpost: Journalpost) {
        val kjøreliste =
            journalpostService.hentSøknadFraJournalpost(journalpost, Stønadstype.DAGLIG_REISE_TSO)
    }
}