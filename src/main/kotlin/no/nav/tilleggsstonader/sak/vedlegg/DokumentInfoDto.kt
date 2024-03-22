package no.nav.tilleggsstonader.sak.vedlegg

import no.nav.tilleggsstonader.kontrakter.journalpost.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.Utsendingsinfo
import no.nav.tilleggsstonader.sak.journalføring.JournalpostDatoUtil.mestRelevanteDato
import java.time.LocalDateTime

data class DokumentInfoDto(
    val dokumentInfoId: String,
    val filnavn: String?,
    val tittel: String,
    val journalpostId: String,
    val dato: LocalDateTime?,
    val tema: String?,
    val journalstatus: Journalstatus,
    val journalposttype: Journalposttype,
    val harSaksbehandlerTilgang: Boolean,
    val logiskeVedlegg: List<LogiskVedleggDto>,
    val avsenderMottaker: AvsenderMottaker?,
    val utsendingsinfo: Utsendingsinfo?,
)

data class LogiskVedleggDto(val tittel: String)

fun tilDokumentInfoDto(
    dokumentInfo: DokumentInfo,
    journalpost: Journalpost,
): DokumentInfoDto {
    return DokumentInfoDto(
        dokumentInfoId = dokumentInfo.dokumentInfoId,
        filnavn = dokumentInfo.dokumentvarianter?.find { it.variantformat == Dokumentvariantformat.ARKIV }?.filnavn,
        tittel = dokumentInfo.tittel ?: "Tittel mangler",
        journalpostId = journalpost.journalpostId,
        dato = mestRelevanteDato(journalpost),
        journalstatus = journalpost.journalstatus,
        journalposttype = journalpost.journalposttype,
        logiskeVedlegg = dokumentInfo.logiskeVedlegg?.map { LogiskVedleggDto(tittel = it.tittel) } ?: emptyList(),
        avsenderMottaker = journalpost.avsenderMottaker,
        utsendingsinfo = journalpost.utsendingsinfo,
        tema = journalpost.tema,
        harSaksbehandlerTilgang = dokumentInfo.dokumentvarianter?.find { it.variantformat == Dokumentvariantformat.ARKIV }?.saksbehandlerHarTilgang ?: false,
    )
}
