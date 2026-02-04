package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.journalføring.JournalføringHelper
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue

@Service
class HåndterMottattKjørelisteService(
    private val journalpostClient: JournalpostClient,
) {
    fun behandleKjøreliste(journalpost: Journalpost) {
        val kjørelisteSkjema =
            hentKjørelisteSkjemaFraJournalpost(journalpost)

        println(kjørelisteSkjema)
    }

    private fun hentKjørelisteSkjemaFraJournalpost(journalpost: Journalpost): KjørelisteSkjema {
        val dokumentBrevkode = journalpost.dokumentBrevkode()
        feilHvis(dokumentBrevkode == null) {
            "Finner ikke dokumentBrevkode for journalpost=${journalpost.journalpostId}"
        }
        val dokumentInfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, dokumentBrevkode)
        val kjørelisteFraJournalpost =
            journalpostClient.hentDokument(
                journalpostId = journalpost.journalpostId,
                dokumentInfoId = dokumentInfo.dokumentInfoId,
                Dokumentvariantformat.ORIGINAL,
            )

        return jsonMapper.readValue<InnsendtSkjema<KjørelisteSkjema>>(kjørelisteFraJournalpost).skjema
    }
}
