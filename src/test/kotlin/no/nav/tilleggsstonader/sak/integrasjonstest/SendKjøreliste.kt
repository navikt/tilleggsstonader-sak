package no.nav.tilleggsstonader.sak.integrasjonstest

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalpostHendelseType
import no.nav.tilleggsstonader.sak.integrasjonstest.dsl.BehandlingTestdataDsl
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

fun IntegrationTest.sendInnKjøreliste(kjøreliste: KjørelisteSkjema): String {
    val journalpostId = Random.nextLong().toString()
    val journalhendelseRecord = journalfoeringHendelseRecord(journalpostId.toLong())

    journalhendelseKafkaListener.listen(
        ConsumerRecordUtil.lagConsumerRecord("key", journalhendelseRecord),
        mockk<Acknowledgment>(relaxed = true),
    )

    mockJournalpost(
        brevkode = DokumentBrevkode.DAGLIG_REISE_KJØRELISTE,
        skjema = InnsendtSkjema("", LocalDateTime.now(), Språkkode.NB, kjøreliste),
        journalpostId = journalpostId.toLong(),
    )

    kjørTasksKlareForProsesseringTilIngenTasksIgjen()

    return journalpostId
}

private fun journalfoeringHendelseRecord(
    journalpostId: Long,
    tema: Tema = Tema.TSO,
) = JournalfoeringHendelseRecord().apply {
    this.journalpostId = journalpostId
    this.mottaksKanal = "NAV_NO"
    this.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
    this.temaNytt = tema.name
    this.hendelsesId = UUID.randomUUID().toString()
}

private fun IntegrationTest.mockJournalpost(
    brevkode: DokumentBrevkode,
    skjema: Any?,
    journalpostKanal: String = "NAV_NO",
    journalpostId: Long,
): Journalpost {
    val dokumentvariantformat = if (skjema != null) Dokumentvariantformat.ORIGINAL else Dokumentvariantformat.ARKIV
    val søknaddookumentInfo =
        dokumentInfo(
            brevkode = brevkode.verdi,
            dokumentvarianter = listOf(dokumentvariant(variantformat = dokumentvariantformat)),
        )
    val journalpost =
        journalpost(
            journalpostId = journalpostId.toString(),
            journalposttype = Journalposttype.I,
            dokumenter = listOf(søknaddookumentInfo),
            kanal = journalpostKanal,
            bruker = Bruker("12345678910", BrukerIdType.FNR),
            journalstatus = Journalstatus.MOTTATT,
        )

    opprettJournalpost(journalpost)

    if (skjema != null) {
        every {
            journalpostClient.hentDokument(
                journalpostId.toString(),
                søknaddookumentInfo.dokumentInfoId,
                Dokumentvariantformat.ORIGINAL,
            )
        } returns
            jsonMapper.writeValueAsBytes(skjema)
    }

    return journalpost
}
