package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
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
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
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
import no.nav.tilleggsstonader.sak.kjøreliste.KjørelisteRepository
import no.nav.tilleggsstonader.sak.kjøreliste.ReisevurderingPrivatBilDto
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import org.springframework.test.web.servlet.client.expectBody
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class InnvilgePrivatBilIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var journalhendelseKafkaListener: JournalhendelseKafkaListener

    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    val fom = 15 september 2025
    val tom = 14 oktober 2025

    @Test
    fun `innvilge rammevedtak privat bil og henter ut rammevedtak`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }
        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingId)

        // Sjekk at ingenting blir utbetalt
        KafkaTestConfig
            .sendteMeldinger()
            .forventAntallMeldingerPåTopic(kafkaTopics.utbetaling, 0)

        // Sjekk at rammevedtaket kan hentes
        val rammevedtak = kall.privatBil.hentRammevedtak(IdentRequest("12345678910"))
        val reiseId = rammevedtak.single().reiseId

        assertThat(rammevedtak).hasSize(1)
        assertThat(rammevedtak.single().fom).isEqualTo(fom)
        assertThat(rammevedtak.single().tom).isEqualTo(tom)

        val dagerKjørt = arrayOf(15 september 2025, 20 september 2025, 23 september 2025)
        val kjøreliste =
            kjørelisteSkjema(
                reiseId = reiseId.toString(),
                periode = Datoperiode(fom, tom),
                dagerKjørt = dagerKjørt,
            )

        // Send inn kjøreliste
        val journalpostId = sendInnKjøreliste(kjøreliste)

        // Verifisere kjøreliste-journalpost blitt arkivert
        verify(exactly = 1) {
            journalpostClient.ferdigstillJournalpost(
                journalpostId = journalpostId,
                journalførendeEnhet = "9999",
                saksbehandler = "VL",
            )
        }

        val lagredeKjørelister = kjørelisteRepository.findByFagsakId(saksbehandling.fagsakId)
        assertThat(lagredeKjørelister).hasSize(1)
        val lagretKjøreliste = lagredeKjørelister.single()
        assertThat(lagretKjøreliste.fagsakId).isEqualTo(saksbehandling.fagsakId)
        assertThat(lagretKjøreliste.journalpostId).isEqualTo(journalpostId)
        assertThat(lagretKjøreliste.data.reiseId).isEqualTo(reiseId)
        assertThat(
            lagretKjøreliste.data.reisedager
                .filter { it.harKjørt }
                .map { it.dato },
        ).containsExactlyInAnyOrder(*dagerKjørt)

        val behandlingerPåFagsak = behandlingRepository.findByFagsakId(saksbehandling.fagsakId)
        assertThat(behandlingerPåFagsak).hasSize(2)
        // TODO - bør behandlingstype si at det er en kjøreliste?
        // TODO - verifiser at behandlingsstatistikk finnes
        val behandlingKjøreliste = behandlingerPåFagsak.single { it.årsak == BehandlingÅrsak.KJØRELISTE }

        // Trigger opprett-oppgave task
        kjørTasksKlareForProsessering()

        val kjørelisteBehandling = behandlingerPåFagsak.single { it.årsak == BehandlingÅrsak.KJØRELISTE }
        assertThat(oppgaveRepository.findByBehandlingId(kjørelisteBehandling.id)).hasSize(1)

        val hentetKjøreliste =
            restTestClient
                .get()
                .uri("/api/kjoreliste/${behandlingKjøreliste.id}")
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<List<ReisevurderingPrivatBilDto>>()
                .returnResult()
                .responseBody

        assertThat(hentetKjøreliste).isNotNull.isNotEmpty
        // Sjekker at alle dager fra kjøreliste kommer i respons
        assertThat(
            hentetKjøreliste!!
                .single()
                .uker
                .flatMap { it.dager }
                .filter { it.dato in dagerKjørt },
        ).allMatch {
            it.kjørelisteDag?.harKjørt == true
        }
    }

    private fun sendInnKjøreliste(kjøreliste: KjørelisteSkjema): String {
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

    private fun mockJournalpost(
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
}
