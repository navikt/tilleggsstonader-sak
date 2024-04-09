package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode

fun Journalpost.harStrukturertSøknad(): Boolean = this.dokumenter?.any {
    it.harStrukturertSøknad()
} ?: false

fun DokumentInfo.harStrukturertSøknad() =
    DokumentBrevkode.erGyldigBrevkode(this.brevkode.toString()) &&
        this.harOriginaldokument()

fun DokumentInfo.harOriginaldokument() = this.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL }
    ?: false

fun Journalpost.harUgyldigAvsenderMottaker(): Boolean = this.journalposttype != Journalposttype.N && this.avsenderMottaker?.navn.isNullOrBlank()

fun Journalpost.manglerAvsenderMottaker(): Boolean = this.avsenderMottaker?.erLikBruker != true && this.avsenderMottaker?.navn.isNullOrBlank() || this.avsenderMottaker?.id.isNullOrBlank()
