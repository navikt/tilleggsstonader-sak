package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import java.util.UUID

object JournalpostUtil {
    fun lagJournalpost(
        id: String = UUID.randomUUID().toString(),
        dokumenter: List<DokumentInfo> = emptyList(),
    ) =
        Journalpost(
            journalpostId = id,
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.FERDIGSTILT,
            dokumenter = dokumenter,
        )

    fun lagDokument(
        dokumentInfoId: String = UUID.randomUUID().toString(),
        vedlegg: List<LogiskVedlegg> = emptyList(),
    ) = DokumentInfo(
        dokumentInfoId = dokumentInfoId,
        logiskeVedlegg = vedlegg,
    )

    fun lagVedlegg(
        logiskVedleggId: String,
        tittel: String,
    ) = LogiskVedlegg(
        logiskVedleggId = logiskVedleggId,
        tittel = tittel,
    )
}
