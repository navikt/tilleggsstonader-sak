package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Status
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.HovedytelseType
import no.nav.tilleggsstonader.kontrakter.søknad.flytting.Flyttemåte
import no.nav.tilleggsstonader.kontrakter.søknad.roah.ROAHTypeReise
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingsjournalpostRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakRepository
import no.nav.tilleggsstonader.sak.hendelser.ConsumerRecordUtil
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalhendelseKafkaHåndtererTask
import no.nav.tilleggsstonader.sak.hendelser.journalføring.JournalpostHendelseType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.Oppgavelager
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.tomYtelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoTiltakspengerTpsak
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil.søknadBoutgifter
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil.søknadDagligReise
import no.nav.tilleggsstonader.sak.util.SøknadFlyttingUtil.søknadFlytting
import no.nav.tilleggsstonader.sak.util.SøknadROAHUtil.søknadROAH
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaLæremidler
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaPassAvBarn
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.dokumentvariant
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import java.util.UUID

class MottaSøknadIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var oppgavelager: Oppgavelager

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var hendelseRepository: HendelseRepository

    @Autowired
    lateinit var oppgaveClient: OppgaveClient

    @Autowired
    lateinit var behandlingsjournalpostRepository: BehandlingsjournalpostRepository

    @Autowired
    lateinit var søknadService: SøknadService

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

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
            ident,
            Stønadstype.BOUTGIFTER,
            journalpostId.toString(),
        )
    }

    @Test
    fun `mottar flere boutgifter-søknader fra kafka, journalføres og oppretter sak`() {
        val hendelse1 = journalfoeringHendelseRecord()
        val hendelse2 = journalfoeringHendelseRecord()
        val hendelse3 = journalfoeringHendelseRecord()

        // Søknad 1 — ingen aktiv behandling, skal opprette behandling og journalføre
        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key1", hendelse1),
            mockk<Acknowledgment>(relaxed = true),
        )
        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse1.hendelsesId)).isNotNull
        mockJournalpost(brevkode = DokumentBrevkode.BOUTGIFTER, søknad = søknadBoutgifter(ident = ident))
        kjørTasksKlareForProsessering()

        // Søknad 2 — aktiv behandling finnes, ønsket atferd: ny behandling settes på vent
        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key2", hendelse2),
            mockk<Acknowledgment>(relaxed = true),
        )
        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse2.hendelsesId)).isNotNull
        mockJournalpost(brevkode = DokumentBrevkode.BOUTGIFTER, søknad = søknadBoutgifter(ident = ident))
        kjørTasksKlareForProsessering()

        // Søknad 3 — aktiv behandling finnes, ønsket atferd: ny behandling settes på vent
        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key3", hendelse3),
            mockk<Acknowledgment>(relaxed = true),
        )
        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse3.hendelsesId)).isNotNull
        mockJournalpost(brevkode = DokumentBrevkode.BOUTGIFTER, søknad = søknadBoutgifter(ident = ident))
        kjørTasksKlareForProsessering()

        val fagsak = fagsakRepository.findBySøkerIdent(setOf(ident)).single()
        assertThat(fagsak.stønadstype).isEqualTo(Stønadstype.BOUTGIFTER)

        val behandlinger = behandlingRepository.findByFagsakId(fagsak.id)

        // Ønsket atferd: 3 søknader → 3 behandlinger, der søknad 2 og 3 er satt på vent
        // Faktisk atferd (bug): kun 1 behandling opprettes — søknad 2 og 3 blir journalføringsoppgaver og mister koblingen til en behandling
        assertThat(behandlinger).hasSize(3)

        val førsteSøknadBehandling = behandlinger.minByOrNull { it.sporbar.opprettetTid }!!
        assertThat(førsteSøknadBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

        val øvrigeBehandlinger = behandlinger.filter { it.id != førsteSøknadBehandling.id }
        assertThat(øvrigeBehandlinger).allSatisfy { behandling ->
            assertThat(behandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
            assertThat(behandling.årsak).isEqualTo(BehandlingÅrsak.SØKNAD)
        }
    }

    @Test
    fun `mottar barnetilsyn-søknad fra kafka, journalføres og oppretter sak`() {
        val hendelse = journalfoeringHendelseRecord()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.PASS_AV_BARN, søknad = søknadskjemaPassAvBarn())
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
            ident,
            Stønadstype.BARNETILSYN,
            journalpostId.toString(),
        )
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

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
            ident,
            Stønadstype.LÆREMIDLER,
            journalpostId.toString(),
        )
    }

    @Test
    fun `mottar to læremidler-søknad på samme bruker fra kafka, journalføres og oppretter to behandlinger hvor siste settes på vent`() {
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

        mockJournalpost(
            brevkode = DokumentBrevkode.LÆREMIDLER,
            søknad = søknadskjemaLæremidler(),
            journalpostId = hendelse1.journalpostId,
        )
        mockJournalpost(
            brevkode = DokumentBrevkode.LÆREMIDLER,
            søknad = søknadskjemaLæremidler(),
            journalpostId = hendelse2.journalpostId,
        )
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val fagsak = fagsakRepository.findBySøkerIdent(setOf(ident)).single()
        assertThat(fagsak.stønadstype).isEqualTo(Stønadstype.LÆREMIDLER)

        val behandlinger = behandlingRepository.findByFagsakId(fagsak.id)
        assertThat(behandlinger).hasSize(2)

        val behandlingSortertPåTid = behandlinger.sortedBy { it.sporbar.opprettetTid }
        assertThat(behandlingSortertPåTid.first().status).isEqualTo(BehandlingStatus.OPPRETTET)
        assertThat(behandlingSortertPåTid.last().status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

        val førsteBehandlingJournalposter = behandlingsjournalpostRepository.findAllByBehandlingId(behandlingSortertPåTid.first().id)
        assertThat(førsteBehandlingJournalposter.map { it.journalpostId }).contains(hendelse1.journalpostId.toString())

        val andreBehandlingJournalposter = behandlingsjournalpostRepository.findAllByBehandlingId(behandlingSortertPåTid.last().id)
        assertThat(andreBehandlingJournalposter.map { it.journalpostId }).contains(hendelse2.journalpostId.toString())
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

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
            ident,
            Stønadstype.DAGLIG_REISE_TSR,
            journalpostId.toString(),
        )
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

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
            ident,
            Stønadstype.DAGLIG_REISE_TSO,
            journalpostId.toString(),
        )
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

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
            ident,
            Stønadstype.DAGLIG_REISE_TSO,
            journalpostId.toString(),
        )
    }

    @Test
    fun `mottar daglig-reise-søknad fra kafka, feiler i oppslag av ytelser, task feiler`() {
        val hendelse = journalfoeringHendelseRecord()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        every { ytelseClient.hentYtelser(any()) } returns
            ytelsePerioderDto(
                kildeResultat =
                    listOf(
                        YtelsePerioderDto.KildeResultatYtelse(
                            TypeYtelsePeriode.AAP,
                            ResultatKilde.FEILET,
                        ),
                    ),
            )

        mockJournalpost(brevkode = DokumentBrevkode.DAGLIG_REISE, søknad = søknadDagligReise())
        kjørTasksKlareForProsessering()

        val journalhendelseTasks = taskService.finnAlleTaskerMedType(JournalhendelseKafkaHåndtererTask.TYPE)
        assertThat(journalhendelseTasks).hasSize(1)
        assertThat(journalhendelseTasks.single().status).isNotEqualTo(Status.FERDIG)
    }

    @Test
    fun `mottar flytting-søknad fra kafka, journalføres og oppretter sak`() {
        val hendelse = journalfoeringHendelseRecord()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        mockJournalpost(brevkode = DokumentBrevkode.FLYTTING, søknad = søknadFlytting(ident = ident))
        kjørTasksKlareForProsessering()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(
            ident,
            Stønadstype.FLYTTING_TSO,
            journalpostId.toString(),
        )
    }

    @Test
    fun `mottar ROAH-TSO-søknad, lagrer skjema og kobler det til behandling`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_OPPSTART_AVSLUTNING_HJEMREISE) } returns true

        val søknad = søknadROAH(ident = ident)
        val vedleggsId =
            søknad.skjema.dokumentasjon
                .single()
                .opplastedeVedlegg
                .single()
                .id
        val vedlegg =
            dokumentInfo(
                dokumentInfoId = "roah-vedlegg-1",
                dokumentvarianter = listOf(dokumentvariant(filnavn = vedleggsId.toString())),
            )
        val hendelse = journalfoeringHendelseRecord(tema = Tema.TSO)
        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )
        mockJournalpost(
            brevkode = DokumentBrevkode.REISE_OPPSTART_AVSLUTNING_ELLER_HJEMREISE,
            søknad = søknad,
            vedlegg = listOf(vedlegg),
        )
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val journalhendelseTasks = taskService.finnAlleTaskerMedType(JournalhendelseKafkaHåndtererTask.TYPE)
        assertThat(journalhendelseTasks).hasSize(1)
        assertThat(journalhendelseTasks.single().status).isEqualTo(Status.FERDIG)

        val fagsak = fagsakRepository.findBySøkerIdent(setOf(ident)).single()
        assertThat(fagsak.stønadstype).isEqualTo(Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO)
        val behandling = behandlingRepository.findByFagsakId(fagsak.id).single()
        assertThat(behandlingsjournalpostRepository.findAllByBehandlingId(behandling.id).map { it.journalpostId })
            .containsExactly(journalpostId.toString())

        val lagret = requireNotNull(søknadService.hentSøknadROAH(behandling.id))
        assertThat(lagret.journalpostId).isEqualTo(journalpostId.toString())
        assertThat(lagret.språk).isEqualTo(Språkkode.NB)
        assertThat(lagret.mottattTidspunkt).isEqualTo((2 mars 2026).atTime(12, 0))
        assertThat(lagret.data.hovedytelse.hovedytelse).containsExactly(Hovedytelse.AAP)
        assertThat(lagret.data.adresser.måBoBorteHjemmefra).isEqualTo(JaNei.JA)
        assertThat(
            lagret.data.adresser.adresseMidlertidigBosted
                ?.landkode,
        ).isEqualTo("NOR")
        assertThat(lagret.data.reiser.map { it.typeReise }).containsExactlyElementsOf(ROAHTypeReise.entries)
        assertThat(lagret.data.reiser.map { it.fom }).containsExactly(3 mars 2026, 4 mars 2026, 7 mars 2026)
        assertThat(lagret.data.reiser.map { it.tom }).containsExactly(null, 6 mars 2026, null)
        assertThat(
            lagret.data.reiser
                .first()
                .reisemåte
                ?.offentligTransport
                ?.totalUtgifterOffentligTransport,
        ).isEqualTo("1200")
        assertThat(
            lagret.data.dokumentasjon
                .single()
                .dokumenter
                .single()
                .dokumentInfoId,
        ).isEqualTo(vedlegg.dokumentInfoId)

        restTestClient
            .get()
            .uri("/api/behandling/${behandling.id}")
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isOk

        val fakta =
            restTestClient
                .get()
                .uri("/api/behandling/${behandling.id}/fakta")
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
        fakta.jsonPath("$['@type']").isEqualTo("REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO")
        fakta.jsonPath("$.søknadMottattTidspunkt").isNotEmpty
        fakta.jsonPath("$.hovedytelse.søknadsgrunnlag.hovedytelse[0]").isEqualTo("AAP")
        fakta.jsonPath("$.dokumentasjon.journalpostId").isEqualTo(journalpostId.toString())
        fakta.jsonPath("$.dokumentasjon.dokumentasjon[0].dokumenter[0].dokumentInfoId").isEqualTo(vedlegg.dokumentInfoId)

        fakta.jsonPath("$.aktivitet").isNotEmpty
        fakta.jsonPath("$.aktivitet.søknadsgrunnlag.aktiviteter").isEmpty
        fakta.jsonPath("$.aktivitet.søknadsgrunnlag.annenAktivitet").isEmpty
        fakta.jsonPath("$.aktivitet.søknadsgrunnlag.lønnetAktivitet").isEmpty
        fakta.jsonPath("$.adresser.måBoBorteHjemmefra").isEqualTo("JA")
        fakta.jsonPath("$.adresser.periodeMidlertidigBosted").isEmpty
        fakta.jsonPath("$.adresser.adresseOriginaltBosted").isEmpty
        fakta.jsonPath("$.adresser.adresseMidlertidigBosted").isNotEmpty
        fakta.jsonPath("$.adresser.adresseMidlertidigBosted.gateadresse").isEmpty
        fakta.jsonPath("$.adresser.adresseMidlertidigBosted.postnummer").isEmpty
        fakta.jsonPath("$.adresser.adresseMidlertidigBosted.poststed").isEmpty
        fakta.jsonPath("$.adresser.adresseMidlertidigBosted.landkode").isEqualTo("NOR")
        fakta.jsonPath("$.barnOgSærligeBehov").isNotEmpty
        fakta.jsonPath("$.barnOgSærligeBehov.harBarnUnder18SomHarFlyttetMed").isEmpty
        fakta.jsonPath("$.barnOgSærligeBehov.hvilkeBarnFlytterMed").isEmpty
        fakta.jsonPath("$.barnOgSærligeBehov.harBarnHjemmeUnder4Klasse").isEmpty
        fakta.jsonPath("$.barnOgSærligeBehov.harSærligeBehovForFlereHjemreiser").isEmpty

        fakta.jsonPath("$.reiser.length()").isEqualTo(3)
        fakta.jsonPath("$.reiser[0].typeReise").isEqualTo("OPPSTART")
        fakta.jsonPath("$.reiser[0].fom").isEqualTo("2026-03-03")
        fakta.jsonPath("$.reiser[0].tom").isEmpty
        fakta.jsonPath("$.reiser[0].reisemåte.hvilkeTransportmidlerBleBenyttet[0]").isEqualTo("OFFENTLIG_TRANSPORT")
        fakta.jsonPath("$.reiser[0].reisemåte.offentligTransport.totalUtgifterOffentligTransport").isEqualTo("1200")
        fakta.jsonPath("$.reiser[1].typeReise").isEqualTo("AVSLUTNING")
        fakta.jsonPath("$.reiser[1].fom").isEqualTo("2026-03-04")
        fakta.jsonPath("$.reiser[1].tom").isEqualTo("2026-03-06")
        fakta.jsonPath("$.reiser[1].reisemåte").isEmpty
        fakta.jsonPath("$.reiser[2].typeReise").isEqualTo("HJEMREISE")
        fakta.jsonPath("$.reiser[2].fom").isEqualTo("2026-03-07")
        fakta.jsonPath("$.reiser[2].tom").isEmpty
        fakta.jsonPath("$.reiser[2].reisemåte").isEmpty
    }

    @ParameterizedTest
    @EnumSource(Flyttemåte::class)
    fun `flyttesøknad fra journalhendelse bevarer alle opplysninger til fakta-api`(flyttemåte: Flyttemåte) {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_FLYTTING) } returns true
        val søknad = søknadFlytting(ident = ident, flyttemåte = flyttemåte, medDokumentasjon = true)
        val vedlegg =
            søknad.skjema.dokumentasjon.flatMap { it.opplastedeVedlegg }.mapIndexed { index, dokument ->
                dokumentInfo(
                    dokumentInfoId = "flyttevedlegg-${index + 1}",
                    dokumentvarianter = listOf(dokumentvariant(filnavn = dokument.id.toString())),
                )
            }
        mockJournalpost(brevkode = DokumentBrevkode.FLYTTING, søknad = søknad, vedlegg = vedlegg)
        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", journalfoeringHendelseRecord()),
            mockk<Acknowledgment>(relaxed = true),
        )
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        validerFinnesBehandlingPåFagsakMedIdentAvTypeMedJournalpostRef(ident, Stønadstype.FLYTTING_TSO, journalpostId.toString())
        val behandling = behandlingRepository.findAll().single()
        val lagret = requireNotNull(søknadService.hentSøknadFlytting(behandling.id))
        assertThat(lagret.data.flyttingDetaljer?.flyttedato).isEqualTo(1 mars 2026)
        assertThat(
            lagret.data.dokumentasjon
                .flatMap { it.dokumenter }
                .map { it.dokumentInfoId },
        ).containsExactlyElementsOf(vedlegg.map { it.dokumentInfoId })

        restTestClient
            .get()
            .uri("/api/behandling/${behandling.id}")
            .medOnBehalfOfToken()
            .exchange()
            .expectStatus()
            .isOk

        val fakta =
            restTestClient
                .get()
                .uri("/api/behandling/${behandling.id}/fakta")
                .medOnBehalfOfToken()
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
        fakta.jsonPath("$['@type']").isEqualTo("FLYTTING_TSO")
        fakta.jsonPath("$.søknadMottattTidspunkt").isNotEmpty
        fakta.jsonPath("$.flyttingDetaljer.årsak").isEqualTo("NY_JOBB")
        fakta.jsonPath("$.flyttingDetaljer.flyttedato").isEqualTo("2026-03-01")
        fakta.jsonPath("$.flyttingDetaljer.adresse.gateadresse").isEqualTo("Mimes vei 1")
        fakta.jsonPath("$.flyttingDetaljer.adresse.postnummer").isEqualTo("5132")
        fakta.jsonPath("$.flyttingDetaljer.adresse.poststed").isEqualTo("Nyborg")
        fakta.jsonPath("$.flyttingDetaljer.adresse.landkode").isEqualTo("NO")
        fakta.jsonPath("$.flyttingDetaljer.skalFlytteFraFolkeregistrertAdresse").isEqualTo("NEI")
        fakta.jsonPath("$.flyttingDetaljer.adresseDetSkalFlyttesFra.gateadresse").isEqualTo("Hjemveien 2")
        fakta.jsonPath("$.flyttingDetaljer.adresseDetSkalFlyttesFra.postnummer").isEqualTo("0123")
        fakta.jsonPath("$.flyttingDetaljer.adresseDetSkalFlyttesFra.poststed").isEqualTo("Oslo")
        fakta.jsonPath("$.flyttingDetaljer.adresseDetSkalFlyttesFra.landkode").isEqualTo("NOR")
        fakta.jsonPath("$.flyttingDetaljer.flyttemåte").isEqualTo(flyttemåte.name)
        fakta.jsonPath("$.flyttingDetaljer.utgifterDekketPåAnnenMåte").isEqualTo("NEI")
        fakta.jsonPath("$.hovedytelse.søknadsgrunnlag.hovedytelse[0]").isEqualTo("AAP")
        fakta.jsonPath("$.dokumentasjon.journalpostId").isEqualTo(journalpostId.toString())
        vedlegg.forEachIndexed { index, dokument ->
            fakta.jsonPath("$.dokumentasjon.dokumentasjon[$index].dokumenter[0].dokumentInfoId").isEqualTo(dokument.dokumentInfoId)
        }
        if (flyttemåte == Flyttemåte.FLYTTER_SELV) {
            fakta.jsonPath("$.flyttingDetaljer.utgifterEgenFlytting.avstand").isEqualTo(120.25)
            fakta.jsonPath("$.flyttingDetaljer.utgifterEgenFlytting.hengerleie").isEqualTo(500)
            fakta.jsonPath("$.flyttingDetaljer.utgifterEgenFlytting.bom").isEqualTo(0)
            fakta.jsonPath("$.flyttingDetaljer.utgifterEgenFlytting.parkering").isEqualTo(25.50)
            fakta.jsonPath("$.flyttingDetaljer.utgifterEgenFlytting.ferje").isEqualTo(100.25)
            fakta.jsonPath("$.flyttingDetaljer.utgifterEgenFlytting.annet").isEmpty
            fakta.jsonPath("$.flyttingDetaljer.flyttetilbud").isEmpty
        } else {
            fakta.jsonPath("$.flyttingDetaljer.utgifterEgenFlytting").isEmpty
            fakta.jsonPath("$.flyttingDetaljer.flyttetilbud.length()").isEqualTo(2)
            fakta.jsonPath("$.flyttingDetaljer.flyttetilbud[0].navn").isEqualTo("Flyttebyrå 1")
            fakta.jsonPath("$.flyttingDetaljer.flyttetilbud[0].beløp").isEqualTo(12000)
            fakta.jsonPath("$.flyttingDetaljer.flyttetilbud[1].navn").isEqualTo("Flyttebyrå 2")
            fakta.jsonPath("$.flyttingDetaljer.flyttetilbud[1].beløp").isEqualTo(10500.50)
        }
    }

    @Test
    fun `mottar scannet reise-til-samling søknad fra kafka, opprettes journalføringsoppgave uten mappetilknytting`() {
        // Når toggle KAN_BEHANDLE_REISE_TIL_SAMLING fjernes, testen oppdateres til reise oppstart, flytting eller reise arbeid,
        // altså andre ikke-støttede søknader/stønadstyper
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_REISE_TIL_SAMLING) } returns false
        val hendelse = journalfoeringHendelseRecord(mottaksKanal = "SKAN_IM")

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        val journalpost =
            mockJournalpost(brevkode = DokumentBrevkode.REISE_TIL_SAMLING, søknad = null, journalpostKanal = "SKAN_IM")
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val oppgaveSlot = mutableListOf<OpprettOppgaveRequest>()
        verify { oppgaveClient.opprettOppgave(capture(oppgaveSlot)) }

        val oppgave =
            oppgaveSlot.first { it.journalpostId == journalpostId.toString() && it.oppgavetype == Oppgavetype.Journalføring }

        assertThat(oppgave.journalpostId).isEqualTo(journalpostId.toString())
        assertThat(oppgave.tema.name).isEqualTo(journalpost.tema)
        assertThat(oppgave.beskrivelse).contains(journalpost.dokumenter?.first()?.tittel)
        assertThat(oppgave.mappeId).isNull()
    }

    @Test
    fun `mottar journalpost som er ferdigstilt, lager task men behandler ikke`() {
        val hendelse = journalfoeringHendelseRecord()

        journalhendelseKafkaListener.listen(
            ConsumerRecordUtil.lagConsumerRecord("key", hendelse),
            mockk<Acknowledgment>(relaxed = true),
        )

        assertThat(hendelseRepository.findByTypeAndId(TypeHendelse.JOURNALPOST, hendelse.hendelsesId)).isNotNull

        val journalpost =
            journalpost(
                journalpostId = journalpostId.toString(),
                journalposttype = Journalposttype.I,
                dokumenter = emptyList(),
                kanal = hendelse.mottaksKanal,
                bruker = Bruker(ident, BrukerIdType.FNR),
                journalstatus = Journalstatus.FERDIGSTILT,
            )
        opprettJournalpost(journalpost)
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
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
        vedlegg: List<DokumentInfo> = emptyList(),
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
                dokumenter = listOf(søknaddookumentInfo) + vedlegg,
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
                jsonMapper.writeValueAsBytes(søknad)
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
