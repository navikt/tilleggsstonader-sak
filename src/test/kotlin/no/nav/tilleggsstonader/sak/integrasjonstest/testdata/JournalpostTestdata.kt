package no.nav.tilleggsstonader.sak.integrasjonstest.testdata

import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.util.journalpost
import java.util.UUID

val defaultJournalpost =
    journalpost(
        journalpostId = "1",
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = listOf(DokumentInfo("", brevkode = DokumentBrevkode.DAGLIG_REISE.verdi)),
        bruker = Bruker("12345678910", BrukerIdType.FNR),
    )

fun journalpostSøknadForStønadstype(stønadstype: Stønadstype): Journalpost {
    val dokumentBrevkode = brevkodeForStønadstype(stønadstype)
    val tema = if (stønadstype == Stønadstype.DAGLIG_REISE_TSR) Tema.TSR else Tema.TSO

    return journalpost(
        journalpostId = "1",
        journalstatus = Journalstatus.MOTTATT,
        dokumenter =
            listOf(
                DokumentInfo(
                    UUID.randomUUID().toString(),
                    brevkode = dokumentBrevkode.verdi,
                    dokumentvarianter =
                        listOf(
                            Dokumentvariant(
                                variantformat = Dokumentvariantformat.ORIGINAL,
                                filnavn = "f",
                                saksbehandlerHarTilgang = true,
                            ),
                        ),
                ),
            ),
        bruker = Bruker("12345678910", BrukerIdType.FNR),
        tema = tema.name,
    )
}

private fun brevkodeForStønadstype(stønadstype: Stønadstype) =
    when (stønadstype) {
        Stønadstype.BARNETILSYN -> DokumentBrevkode.BARNETILSYN
        Stønadstype.LÆREMIDLER -> DokumentBrevkode.LÆREMIDLER
        Stønadstype.BOUTGIFTER -> DokumentBrevkode.BOUTGIFTER
        Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR -> DokumentBrevkode.DAGLIG_REISE
    }
