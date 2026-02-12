package no.nav.tilleggsstonader.sak.integrasjonstest.extensions

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.JournalpostClientMockConfig.Companion.journalposter
import no.nav.tilleggsstonader.sak.util.journalpostMedStrukturertSøknad

fun opprettJournalpost(journalpost: Journalpost = journalpostMedStrukturertSøknad(DokumentBrevkode.BOUTGIFTER)): Journalpost {
    journalposter[journalpost.journalpostId.toLong()] = journalpost
    return journalpost
}
