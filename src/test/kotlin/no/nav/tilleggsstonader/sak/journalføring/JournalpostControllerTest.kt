package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.JournalpostClientMockConfig.Companion.journalposter
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.fullførJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.hentJournalpost
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.klage.KlageClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil.søknadBoutgifter
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil.søknadDagligReise
import no.nav.tilleggsstonader.sak.util.journalpostMedStrukturertSøknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.collections.set

class JournalpostControllerTest(
    @Autowired val fagsakService: FagsakService,
    @Autowired val behandlingService: BehandlingService,
    @Autowired val klageClient: KlageClient,
    @Autowired val journalpostClient: JournalpostClient,
    @Autowired val oppgaveClient: OppgaveClient,
) : IntegrationTest() {
    val ident = "12345678910"
    val saksbehandler = "ole"
    val enhet = "enhet"

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettPerson(ident)
    }

    @Nested
    inner class FullførJournalpost {
        @Test
        fun `fullfør journalpost - skal ferdigstille journalpost, og opprette behandling og oppgave`() {
            val journalpostId =
                medBrukercontext(bruker = saksbehandler) {
                    fullførJournalpost(
                        "1",
                        JournalføringRequest(
                            stønadstype = Stønadstype.BARNETILSYN,
                            aksjon = JournalføringRequest.Journalføringsaksjon.OPPRETT_BEHANDLING,
                            årsak = JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD,
                            oppgaveId = "123",
                            journalførendeEnhet = enhet,
                            logiskeVedlegg = mapOf("1" to listOf(LogiskVedlegg("1", "ny tittel"))),
                        ),
                    )
                }

            assertThat(journalpostId).isEqualTo("1")

            val fagsak = fagsakService.finnFagsak(setOf(ident), Stønadstype.BARNETILSYN)
            assertThat(fagsak).isNotNull

            val behandlinger = behandlingService.hentBehandlinger(fagsak!!.id)
            assertThat(behandlinger).hasSize(1)

            val opprettetBehandling = behandlinger.first()
            assertThat(opprettetBehandling.årsak).isEqualTo(JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD.behandlingsårsak)
            assertThat(opprettetBehandling.steg).isEqualTo(StegType.INNGANGSVILKÅR)
            assertThat(opprettetBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

            val opprettedeTasks = taskService.findAll()
            assertThat(opprettedeTasks).hasSize(2)

            val bahandlesakOppgaveTask =
                opprettedeTasks.single { it.type == OpprettOppgaveForOpprettetBehandlingTask.TYPE }
            val behandlesakOppgavePayload =
                objectMapper.readValue<OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData>(
                    bahandlesakOppgaveTask.payload,
                )
            assertThat(behandlesakOppgavePayload.behandlingId).isEqualTo(opprettetBehandling.id)

            verify(exactly = 1) { journalpostClient.ferdigstillJournalpost("1", enhet, saksbehandler) }
            verify(exactly = 1) {
                journalpostClient.oppdaterLogiskeVedlegg(
                    dokumentInfoId = "1",
                    request = BulkOppdaterLogiskVedleggRequest(listOf("ny tittel")),
                )
            }
            verify(exactly = 1) { oppgaveClient.ferdigstillOppgave("123".toLong()) }
        }

        @Test
        fun `fullfør journalpost - skal ferdigstille journalpost, og opprette klage`() {
            val journalpostId =
                medBrukercontext(bruker = saksbehandler) {
                    fullførJournalpost(
                        "1",
                        JournalføringRequest(
                            stønadstype = Stønadstype.BARNETILSYN,
                            aksjon = JournalføringRequest.Journalføringsaksjon.OPPRETT_BEHANDLING,
                            årsak = JournalføringRequest.Journalføringsårsak.KLAGE,
                            oppgaveId = "123",
                            journalførendeEnhet = enhet,
                            logiskeVedlegg = mapOf("1" to listOf(LogiskVedlegg("1", "ny tittel"))),
                        ),
                    )
                }

            assertThat(journalpostId).isEqualTo("1")

            val fagsak = fagsakService.finnFagsak(setOf(ident), Stønadstype.BARNETILSYN)
            assertThat(fagsak).isNotNull

            val behandlinger = behandlingService.hentBehandlinger(fagsak!!.id)
            assertThat(behandlinger).hasSize(0)

            verify(exactly = 1) {
                klageClient.opprettKlage(
                    OpprettKlagebehandlingRequest(
                        ident = "12345678910",
                        stønadstype = Stønadstype.BARNETILSYN,
                        eksternFagsakId = fagsak.eksternId.id.toString(),
                        fagsystem = Fagsystem.TILLEGGSSTONADER,
                        klageMottatt = LocalDate.now().minusDays(7),
                        behandlendeEnhet = "4462",
                    ),
                )
            }

            verify(exactly = 1) { journalpostClient.ferdigstillJournalpost("1", enhet, saksbehandler) }
            verify(exactly = 1) {
                journalpostClient.oppdaterLogiskeVedlegg(
                    dokumentInfoId = "1",
                    request = BulkOppdaterLogiskVedleggRequest(listOf("ny tittel")),
                )
            }
            verify(exactly = 1) { oppgaveClient.ferdigstillOppgave("123".toLong()) }
        }
    }

    @Nested
    inner class HentJournalpost {
        @Test
        fun `hent journalpost som inneholder søknad daglig reise, kan kun opprette stønadstyper for daglig reise`() {
            val journalpost = journalpostMedStrukturertSøknad(DokumentBrevkode.DAGLIG_REISE)
            leggTilJournalpostMedSøknadIMock(journalpost, objectMapper.writeValueAsBytes(søknadDagligReise()))

            val journalpostResponse = hentJournalpost(journalpost.journalpostId)

            assertThat(journalpostResponse.defaultStønadstype).isEqualTo(Stønadstype.DAGLIG_REISE_TSO)

            assertThat(journalpostResponse.valgbareStønadstyper).containsExactlyInAnyOrder(
                Stønadstype.DAGLIG_REISE_TSO,
                Stønadstype.DAGLIG_REISE_TSR,
            )
        }

        @Test
        fun `hent journalpost som inneholder søknad boutgifter, kan kunn opprette stønadstype boutgifter`() {
            val journalpost = journalpostMedStrukturertSøknad(DokumentBrevkode.BOUTGIFTER)
            leggTilJournalpostMedSøknadIMock(journalpost, objectMapper.writeValueAsBytes(søknadBoutgifter()))

            val journalpostResponse = hentJournalpost(journalpost.journalpostId)

            assertThat(journalpostResponse.valgbareStønadstyper).containsExactly(
                Stønadstype.BOUTGIFTER,
            )
        }

        // Journalpost må ha dokumentinfo med variant ORIGINAL
        fun leggTilJournalpostMedSøknadIMock(
            journalpost: Journalpost,
            originaldokument: ByteArray,
        ) {
            journalposter[journalpost.journalpostId.toLong()] = journalpost
            val dokumentInfoIdMedOriginalVariant =
                journalpost.dokumenter
                    ?.first { dokumentInfo ->
                        dokumentInfo.dokumentvarianter?.any { it.variantformat == Dokumentvariantformat.ORIGINAL } == true
                    }?.dokumentInfoId ?: error("Journalpost mangler dokument med variant ORIGINAL")

            every {
                journalpostClient.hentDokument(
                    journalpost.journalpostId,
                    dokumentInfoIdMedOriginalVariant,
                    Dokumentvariantformat.ORIGINAL,
                )
            } returns
                originaldokument
        }
    }
}
