package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.behandlendeEnhet
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.IdentRequest
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalhendelseKafkaListener
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalpostHendelseType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaTestConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.forventAntallMeldingerPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.SøknadKjørelisteUtil.søknadKjøreliste
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import java.util.UUID

class InnvilgePrivatBilIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var journalhendelseKafkaListener: JournalhendelseKafkaListener

    val fom = 15 september 2025
    val tom = 14 oktober 2025

    @Test
    fun `innvilge rammevedtak privat bil og henter ut rammevedtak`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)
        }

        // Sjekk at ingenting blir utbetalt
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        // Sjekk at rammevedtaket kan hentes
        val rammevedtak = kall.privatBil.hentRammevedtak(IdentRequest("12345678910"))

        assertThat(rammevedtak).hasSize(1)
        assertThat(rammevedtak.single().fom).isEqualTo(fom)
        assertThat(rammevedtak.single().tom).isEqualTo(tom)

        // Send inn kjøreliste
        val journalpostId = sendInnKjøreliste()

        // Verifisere kjøreliste-journalpost blitt arkivert
        verify(exactly = 1) {
            journalpostClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = Stønadstype.DAGLIG_REISE_TSO.behandlendeEnhet().enhetsnr,
                saksbehandler = null,
            )
        }
    }

    private fun sendInnKjøreliste(): String {
        val journalpostId = "123321"
        val journalhendelseRecord = journalfoeringHendelseRecord(journalpostId.toLong())

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", journalhendelseRecord),
            mockk<Acknowledgment>(relaxed = true),
        )

        mockJournalpost(
            brevkode = DokumentBrevkode.DAGLIG_REISE_KJØRELISTE,
            søknad = søknadKjøreliste(),
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

    private fun mockJournalpost(
        brevkode: DokumentBrevkode,
        søknad: Any?,
        journalpostKanal: String = "NAV_NO",
        journalpostId: Long,
    ): Journalpost {
        val dokumentvariantformat = if (søknad != null) Dokumentvariantformat.ORIGINAL else Dokumentvariantformat.ARKIV
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

        if (søknad != null) {
            every {
                journalpostClient.hentDokument(
                    journalpostId.toString(),
                    søknaddookumentInfo.dokumentInfoId,
                    Dokumentvariantformat.ORIGINAL,
                )
            } returns
                jsonMapper.writeValueAsBytes(søknad)
        }

        return journalpost
    }
}
