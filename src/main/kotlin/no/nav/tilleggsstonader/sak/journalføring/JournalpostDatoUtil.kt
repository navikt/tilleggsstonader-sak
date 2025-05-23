package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import java.time.LocalDateTime

object JournalpostDatoUtil {
    fun mestRelevanteDato(journalpost: Journalpost): LocalDateTime? =
        journalpost.datoMottatt ?: journalpost.relevanteDatoer?.maxByOrNull { datoTyperSortert(it.datotype) }?.dato

    private fun datoTyperSortert(datoType: String) =
        when (datoType) {
            "DATO_JOURNALFOERT" -> 4
            "DATO_REGISTRERT" -> 3
            "DATO_DOKUMENT" -> 2
            "DATO_OPPRETTET" -> 1
            else -> 0
        }
}
