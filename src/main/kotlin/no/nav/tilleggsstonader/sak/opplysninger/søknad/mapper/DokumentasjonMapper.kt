package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.Skjema
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Vedlegg

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
                vedlegg = it.opplastedeVedlegg.map { dokument ->
                    val dokumentId = dokument.id
                    vedlegg[dokumentId.toString()]
                        ?: error("Finner ikke logisk vedlegg i journalpost=${journalpost.journalpostId} med tittel=$dokumentId")
                },
                identBarn = it.barnId,
            )
        }
    }

    private fun mapVedleggPåId(journalpost: Journalpost): Map<String, Vedlegg> {
        return journalpost.dokumenter?.mapNotNull {
            it.logiskeVedlegg?.map { it.tittel to Vedlegg(id = it.logiskVedleggId) }
        }?.flatten()?.toMap() ?: emptyMap()
    }
}
