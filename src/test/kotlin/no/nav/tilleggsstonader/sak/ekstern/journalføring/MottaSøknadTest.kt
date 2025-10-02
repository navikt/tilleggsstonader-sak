package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingsjournalpostRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalhendelseKafkaListener
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalpostHendelseType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil.søknadBoutgifter
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil.søknadDagligReise
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaLæremidler
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import java.util.UUID

class MottaSøknadTest : IntegrationTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var journalpostClient: JournalpostClient

    @Autowired
    lateinit var journalhendelseKafkaListener: JournalhendelseKafkaListener

    @Autowired
    lateinit var hendelseRepository: HendelseRepository

    @Autowired
    lateinit var oppgaveClient: OppgaveClient

    @Autowired
    lateinit var behandlingsjournalpostRepository: BehandlingsjournalpostRepository

    val journalpostId = 123321L
    val ident = "12345678901"

    @Test
    fun `mottar boutgifter-søknad fra kafka, journalføres og oppretter sak`() {
        val hendelse = JournalfoeringHendelseRecord()
        hendelse.journalpostId = journalpostId
        hendelse.mottaksKanal = "NAV_NO"
        hendelse.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        hendelse.temaNytt = Tema.TSO.name
        hendelse.hendelsesId = UUID.randomUUID().toString()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.BOUTGIFTER, søknad = søknadBoutgifter(ident = ident))
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.BOUTGIFTER, journalpostId.toString())
    }

    @Test
    fun `mottar barnetilsyn-søknad fra kafka, journalføres og oppretter sak`() {
        val hendelse = JournalfoeringHendelseRecord()
        hendelse.journalpostId = journalpostId
        hendelse.mottaksKanal = "NAV_NO"
        hendelse.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        hendelse.temaNytt = Tema.TSO.name
        hendelse.hendelsesId = UUID.randomUUID().toString()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.BARNETILSYN, søknad = søknadskjemaBarnetilsyn())
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.BARNETILSYN, journalpostId.toString())
    }

    @Test
    fun `mottar læremidler-søknad fra kafka, journalføres og oppretter sak`() {
        val hendelse = JournalfoeringHendelseRecord()
        hendelse.journalpostId = journalpostId
        hendelse.mottaksKanal = "NAV_NO"
        hendelse.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        hendelse.temaNytt = Tema.TSO.name
        hendelse.hendelsesId = UUID.randomUUID().toString()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.LÆREMIDLER, søknad = søknadskjemaLæremidler())
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.LÆREMIDLER, journalpostId.toString())
    }

    @Test
    fun `mottar daglig-resise-søknad fra kafka, journalføres og oppretter sak på TSR`() {
        val hendelse = JournalfoeringHendelseRecord()
        hendelse.journalpostId = journalpostId
        hendelse.mottaksKanal = "NAV_NO"
        hendelse.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        // Daglig reise TSR har tema TSO fra FyllUtSendInn. De blir flyttet over til TSR i journalføringen
        hendelse.temaNytt = Tema.TSO.name
        hendelse.hendelsesId = UUID.randomUUID().toString()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        val hovedytelse = mapOf(HovedytelseType.tiltakspenger to true)
        mockJournalpost(brevkode = DokumentBrevkode.DAGLIG_REISE, søknad = søknadDagligReise(hovedytelse = hovedytelse))
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.DAGLIG_REISE_TSR, journalpostId.toString())
    }

    @Test
    fun `mottar daglig-resise-søknad fra kafka, journalføres og oppretter sak på TSO`() {
        val hendelse = JournalfoeringHendelseRecord()
        hendelse.journalpostId = journalpostId
        hendelse.mottaksKanal = "NAV_NO"
        hendelse.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        hendelse.temaNytt = Tema.TSO.name
        hendelse.hendelsesId = UUID.randomUUID().toString()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.DAGLIG_REISE, søknad = søknadDagligReise())
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.DAGLIG_REISE_TSO, journalpostId.toString())
    }

    @Test
    fun `mottar daglig-reise-ettersendelse fra kafka, opprettes journalføringsoppgave uten mappetilknytting`() {
        val hendelse = JournalfoeringHendelseRecord()
        hendelse.journalpostId = journalpostId
        hendelse.mottaksKanal = "SKAN_IM"
        hendelse.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        hendelse.temaNytt = Tema.TSO.name
        hendelse.hendelsesId = UUID.randomUUID().toString()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        val journalpost = mockJournalpost(brevkode = DokumentBrevkode.DAGLIG_REISE, søknad = null, journalpostKanal = "SKAN_IM")
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val oppgaveSlot = mutableListOf<OpprettOppgaveRequest>()
        verify { oppgaveClient.opprettOppgave(capture(oppgaveSlot)) }

        val oppgave = oppgaveSlot.first { it.journalpostId == journalpostId.toString() }

        assertThat(oppgave.journalpostId).isEqualTo(journalpostId.toString())
        assertThat(oppgave.tema.name).isEqualTo(journalpost.tema)
        assertThat(oppgave.beskrivelse).contains(journalpost.dokumenter?.first()?.tittel)
        assertThat(oppgave.mappeId).isNull()
    }

    private fun mockJournalpost(
        brevkode: DokumentBrevkode,
        søknad: Any?,
        journalpostKanal: String = "NAV_NO",
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
                bruker = Bruker(ident, BrukerIdType.FNR),
                journalstatus = Journalstatus.MOTTATT,
            )

        every { journalpostClient.hentJournalpost(journalpostId.toString()) } returns journalpost

        if (søknad != null) {
            every {
                journalpostClient.hentDokument(
                    journalpostId.toString(),
                    søknaddookumentInfo.dokumentInfoId,
                    Dokumentvariantformat.ORIGINAL,
                )
            } returns
                objectMapper.writeValueAsBytes(søknad)
        }

        return journalpost
    }

    private fun validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
        ident: String,
        stønadstype: Stønadstype,
        journalpostId: String,
    ) {
        val fagsakerPåBruker = fagsakRepository.findBySøkerIdent(setOf(ident))
        assertThat(fagsakerPåBruker).hasSize(1)
        val fagsak = fagsakerPåBruker.single()
        assertThat(fagsak.stønadstype).isEqualTo(stønadstype)

        val behandlinger = behandlingRepository.findByFagsakId(fagsak.id)
        assertThat(behandlinger).hasSize(1)
        val behandling = behandlinger.single()
        assertThat(behandling.årsak).isEqualTo(BehandlingÅrsak.SØKNAD)

        val behandlingsJournalpost = behandlingsjournalpostRepository.findAllByBehandlingId(behandling.id)
        assertThat(behandlingsJournalpost).hasSize(1)
        assertThat(behandlingsJournalpost.first().journalpostId).isEqualTo(journalpostId)
    }
}
