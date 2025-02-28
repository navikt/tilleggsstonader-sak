package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.dokarkiv.DokarkivBruker
import no.nav.tilleggsstonader.kontrakter.dokarkiv.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.dokarkiv.OppdaterJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Sak
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import org.springframework.http.HttpStatus
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo as DokumentInfoJournalpost

object JournalføringHelper {
    /**
     * [Journalposttype.N] brukes for innskannede dokumentm, samme validering finnes i dokarkiv
     */
    fun validerMottakerFinnes(journalpost: Journalpost) {
        brukerfeilHvis(journalpost.harUgyldigAvsenderMottaker()) {
            "Avsender mangler og må settes på journalposten i gosys. " +
                "Når endringene er gjort, trykker du på \"Lagre utkast\" før du går tilbake og journalfører."
        }
    }

    fun plukkUtOriginaldokument(
        journalpost: Journalpost,
        dokumentBrevkode: DokumentBrevkode,
    ): DokumentInfoJournalpost {
        val dokumenter = journalpost.dokumenter ?: error("Fant ingen dokumenter på journalposten")
        return dokumenter.firstOrNull {
            DokumentBrevkode.erGyldigBrevkode(it.brevkode.toString()) &&
                dokumentBrevkode == DokumentBrevkode.fraBrevkode(it.brevkode.toString()) &&
                harOriginalDokument(it)
        } ?: throw ApiFeil("Det finnes ingen søknad i journalposten for å opprette en ny behandling", HttpStatus.BAD_REQUEST)
    }

    private fun harOriginalDokument(dokument: DokumentInfoJournalpost): Boolean =
        dokument.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL }
            ?: false

    fun lagOppdaterJournalpostRequest(
        journalpost: Journalpost,
        eksternFagsakId: Long,
        dokumenttitler: Map<String, String>?,
        nyAvsender: AvsenderMottaker?,
    ) = OppdaterJournalpostRequest(
        avsenderMottaker = nyAvsender,
        bruker =
            journalpost.bruker?.let {
                DokarkivBruker(idType = BrukerIdType.valueOf(it.type.toString()), id = it.id)
            },
        tema = journalpost.tema?.let { Tema.valueOf(it) },
        tittel = journalpost.tittel,
        journalfoerendeEnhet = journalpost.journalforendeEnhet,
        sak =
            Sak(
                fagsakId = eksternFagsakId.toString(),
                fagsaksystem = Fagsystem.TILLEGGSSTONADER,
                sakstype = "FAGSAK",
            ),
        dokumenter =
            dokumenttitler?.let {
                journalpost.dokumenter?.map { dokumentInfo ->
                    DokumentInfo(
                        dokumentInfoId = dokumentInfo.dokumentInfoId,
                        tittel =
                            dokumenttitler[dokumentInfo.dokumentInfoId]
                                ?: dokumentInfo.tittel,
                        brevkode = dokumentInfo.brevkode,
                    )
                }
            },
    )

    /**
     * Frontend sender inn avsenderMottaker som en del av journalføringsrequesten.
     *
     * Dersom avsender er lik bruker populeres id, idType og navn i nyAvsender fra frontend.
     *
     * Dersom det er en annen avsender enn bruker, oppgis bare navn.
     */
    fun JournalføringRequest.NyAvsender.tilAvsenderMottaker(): AvsenderMottaker {
        feilHvis(erBruker && personIdent.isNullOrBlank()) {
            "Mangler personident på avsender"
        }
        feilHvis(navn.isNullOrBlank()) {
            "Mangler navn på avsender"
        }
        return AvsenderMottaker(
            id = personIdent,
            idType = if (personIdent != null) BrukerIdType.FNR else null,
            navn = navn,
        )
    }
}
