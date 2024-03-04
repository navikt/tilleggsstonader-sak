package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.Skjema
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokument

object DokumentasjonMapper {
    /**
     * Vedlegg i journalposten blir lagrede med en id i tittel for å kunne finne de
     * Lagrer [Dokumentasjon] i databasen med referanse til vedlegget i journalposten, for å enkelt kunne åpne det fra frontend
     */
    fun mapDokumentasjon(skjema: Skjema, journalpost: Journalpost): List<Dokumentasjon> {
        val vedlegg = mapVedleggPåId(journalpost)

        return skjema.dokumentasjon.map {
            Dokumentasjon(
                type = it.type,
                harSendtInn = it.harSendtInn,
                dokumenter = it.opplastedeVedlegg.map { dokument ->
                    val dokumentId = dokument.id
                    vedlegg[dokumentId.toString()]
                        ?: error("Finner ikke vedlegg i journalpost=${journalpost.journalpostId} med tittel=$dokumentId")
                },
                identBarn = it.barnId,
            )
        }
    }

    private fun mapVedleggPåId(journalpost: Journalpost): Map<String, Dokument> {
        return journalpost.dokumenter?.mapNotNull { dokumentInfo ->
            dokumentInfo.tittel?.let { it to Dokument(dokumentInfoId = dokumentInfo.dokumentInfoId) }
        }?.toMap() ?: emptyMap()
    }
}
