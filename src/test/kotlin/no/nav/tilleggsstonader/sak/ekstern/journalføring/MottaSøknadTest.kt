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
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingsjournalpostRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalhendelseKafkaListener
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalpostHendelseType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.Oppgavelager
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.tomYtelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
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

class MottaSøknadTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var oppgavelager: Oppgavelager

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var journalhendelseKafkaListener: JournalhendelseKafkaListener

    @Autowired
    lateinit var hendelseRepository: HendelseRepository

    @Autowired
    lateinit var oppgaveClient: OppgaveClient

    @Autowired
    lateinit var behandlingsjournalpostRepository: BehandlingsjournalpostRepository

    @Autowired
    lateinit var ytelseClient: YtelseClient

    val journalpostId = 123321L
    val ident = "12345678901"

    @Test
    fun `mottar boutgifter-søknad fra kafka, journalføres og oppretter sak`() {
        val hendelse = journalfoeringHendelseRecord()

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
        val hendelse = journalfoeringHendelseRecord()

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
        val hendelse = journalfoeringHendelseRecord()

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
    fun `mottar to læremidler-søknad på samme bruker fra kafka, journalføres og oppretter sak og en jfr-oppgave`() {
        val hendelse1 = journalfoeringHendelseRecord(journalpostId = 67)
        val hendelse2 = journalfoeringHendelseRecord(journalpostId = 6767)

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key1", hendelse1),
            mockk<Acknowledgment>(relaxed = true),
        )
        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key2", hendelse2),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse1.hendelsesId)).isNotNull
        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse2.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.LÆREMIDLER, søknad = søknadskjemaLæremidler(), journalpostId = hendelse1.journalpostId)
        mockJournalpost(brevkode = DokumentBrevkode.LÆREMIDLER, søknad = søknadskjemaLæremidler(), journalpostId = hendelse2.journalpostId)
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.LÆREMIDLER, hendelse1.journalpostId.toString())

        // Verifiserer at det har blitt opprettet jfr-oppgave for den andre søknaden
        assertThat(
            oppgavelager.alleOppgaver().filter {
                it.journalpostId?.toLong() == hendelse2.journalpostId &&
                    it.oppgavetype == "JFR"
            },
        ).hasSize(1)
    }

    @Test
    fun `mottar daglig-reise-søknad fra kafka, bruker mottar tiltakspenger, journalføres og oppretter sak på TSR`() {
        val hendelse = journalfoeringHendelseRecord()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoTiltakspengerTpsak()

        mockJournalpost(brevkode = DokumentBrevkode.DAGLIG_REISE, søknad = søknadDagligReise())
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.DAGLIG_REISE_TSR, journalpostId.toString())
    }

    @Test
    fun `mottar daglig-resise-søknad fra kafka, bruker mottar AAP, journalføres og oppretter sak på TSO`() {
        val hendelse = journalfoeringHendelseRecord()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoAAP()
        mockJournalpost(brevkode = DokumentBrevkode.DAGLIG_REISE, søknad = søknadDagligReise())
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.DAGLIG_REISE_TSO, journalpostId.toString())
    }

    @Test
    fun `mottar daglig-reise-søknad fra kafka på person uten ytelser i register, journalføres og oppretter sak på TSO`() {
        val hendelse = journalfoeringHendelseRecord()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        every { ytelseClient.hentYtelser(any()) } returns tomYtelsePerioderDto()
        // Default AAP
        mockJournalpost(
            brevkode = DokumentBrevkode.DAGLIG_REISE,
            søknad = søknadDagligReise(mapOf(HovedytelseType.arbeidsavklaringspenger to true)),
        )
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.DAGLIG_REISE_TSO, journalpostId.toString())
    }

    @Test
    fun `mottar scannet daglig-reise søknad fra kafka, opprettes journalføringsoppgave uten mappetilknytting`() {
        val hendelse = journalfoeringHendelseRecord(mottaksKanal = "SKAN_IM")

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        val journalpost = mockJournalpost(brevkode = DokumentBrevkode.DAGLIG_REISE, søknad = null, journalpostKanal = "SKAN_IM")
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val oppgaveSlot = mutableListOf<OpprettOppgaveRequest>()
        verify { oppgaveClient.opprettOppgave(capture(oppgaveSlot)) }

        val oppgave = oppgaveSlot.first { it.journalpostId == journalpostId.toString() && it.oppgavetype == Oppgavetype.Journalføring }

        assertThat(oppgave.journalpostId).isEqualTo(journalpostId.toString())
        assertThat(oppgave.tema.name).isEqualTo(journalpost.tema)
        assertThat(oppgave.beskrivelse).contains(journalpost.dokumenter?.first()?.tittel)
        assertThat(oppgave.mappeId).isNull()
    }

    private fun journalfoeringHendelseRecord(
        journalpostId: Long = this.journalpostId,
        mottaksKanal: String = "NAV_NO",
        tema: Tema = Tema.TSO,
    ) = JournalfoeringHendelseRecord().apply {
        this.journalpostId = journalpostId
        this.mottaksKanal = mottaksKanal
        this.hendelsesType = JournalpostHendelseType.JournalpostMottatt.name
        this.temaNytt = tema.name
        this.hendelsesId = UUID.randomUUID().toString()
    }

    private fun mockJournalpost(
        brevkode: DokumentBrevkode,
        søknad: Any?,
        journalpostKanal: String = "NAV_NO",
        journalpostId: Long = this.journalpostId,
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

        opprettJournalpost(journalpost)

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
