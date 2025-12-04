package no.nav.tilleggsstonader.sak.journalføring

import io.mockk.every
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.journalpost.LogiskVedlegg
import no.nav.tilleggsstonader.kontrakter.klage.OpprettKlagebehandlingRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.JournalpostClientMockConfig.Companion.journalposter
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalføringsoppgave
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettJournalpost
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsessering
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.klage.KlageClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDtoAAP
import no.nav.tilleggsstonader.sak.util.SøknadBoutgifterUtil.søknadBoutgifter
import no.nav.tilleggsstonader.sak.util.SøknadDagligReiseUtil.søknadDagligReise
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.dokumentInfo
import no.nav.tilleggsstonader.sak.util.journalpost
import no.nav.tilleggsstonader.sak.util.journalpostMedStrukturertSøknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate

class JournalpostControllerTest(
    @Autowired val fagsakService: FagsakService,
    @Autowired val behandlingService: BehandlingService,
    @Autowired val klageClient: KlageClient,
    @Autowired val journalpostClient: JournalpostClient,
    @Autowired val oppgaveClient: OppgaveClient,
    @Autowired val ytelseClient: YtelseClient,
) : CleanDatabaseIntegrationTest() {
    val ident = "12345678910"
    val saksbehandler = "ole"

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettPerson(ident)
    }

    @Nested
    inner class FullførJournalpost {
        @Test
        fun `fullfør journalpost - skal ferdigstille journalpost, og opprette behandling og oppgave`() {
            val journalpost = opprettJournalpost(journalpostMedStrukturertSøknad(DokumentBrevkode.BARNETILSYN))
            leggTilJournalpostMedSøknadIMock(journalpost, jsonMapper.writeValueAsBytes(søknadskjemaBarnetilsyn()))
            val dokumentInfoId = journalpost.dokumenter!!.single().dokumentInfoId
            val oppgave = opprettJournalføringsoppgave(journalpostId = journalpost.journalpostId)
            val journalpostId =
                medBrukercontext(bruker = saksbehandler) {
                    kall.journalpost.fullfor(
                        journalpostId = oppgave.journalpostId!!,
                        request =
                            JournalføringRequest(
                                stønadstype = Stønadstype.BARNETILSYN,
                                aksjon = JournalføringRequest.Journalføringsaksjon.OPPRETT_BEHANDLING,
                                årsak = JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD,
                                oppgaveId = oppgave.id.toString(),
                                logiskeVedlegg = mapOf(dokumentInfoId to listOf(LogiskVedlegg(dokumentInfoId, "ny tittel"))),
                            ),
                    )
                }

            assertThat(journalpostId).isEqualTo(oppgave.journalpostId)

            val fagsak = fagsakService.finnFagsak(setOf(ident), Stønadstype.BARNETILSYN)
            assertThat(fagsak).isNotNull

            val behandlinger = behandlingService.hentBehandlinger(fagsak!!.id)
            assertThat(behandlinger).hasSize(1)

            val opprettetBehandling = behandlinger.first()
            assertThat(opprettetBehandling.årsak).isEqualTo(JournalføringRequest.Journalføringsårsak.DIGITAL_SØKNAD.behandlingsårsak)
            assertThat(opprettetBehandling.steg).isEqualTo(StegType.INNGANGSVILKÅR)
            assertThat(opprettetBehandling.status).isEqualTo(BehandlingStatus.OPPRETTET)

            kjørTasksKlareForProsessering()
            assertThat(mockClientService.oppgavelager.hentOppgave(oppgave.id).status).isEqualTo(StatusEnum.FERDIGSTILT)
            assertThat(
                mockClientService.journalpostClient.hentJournalpost(journalpostId).journalstatus,
            ).isEqualTo(Journalstatus.FERDIGSTILT)
            assertThat(oppgaveRepository.findByBehandlingId(opprettetBehandling.id).filter { it.erBehandlingsoppgave() }).hasSize(1)

            verify(exactly = 1) { journalpostClient.ferdigstillJournalpost(journalpostId, oppgave.tildeltEnhetsnr!!, saksbehandler) }
            verify(exactly = 1) {
                journalpostClient.oppdaterLogiskeVedlegg(
                    dokumentInfoId = dokumentInfoId,
                    request = BulkOppdaterLogiskVedleggRequest(listOf("ny tittel")),
                )
            }
            verify(exactly = 1) { oppgaveClient.ferdigstillOppgave(oppgave.id, any()) }
        }

        @Test
        fun `fullfør journalpost - skal ferdigstille journalpost, og opprette klage`() {
            val journalpost =
                opprettJournalpost(
                    journalpost(
                        bruker = Bruker(ident, BrukerIdType.FNR),
                        dokumenter = listOf(dokumentInfo()),
                    ),
                )
            val oppgave = opprettJournalføringsoppgave(journalpostId = journalpost.journalpostId)
            val dokumentInfoId = journalpost.dokumenter!!.single().dokumentInfoId
            val journalpostId =
                medBrukercontext(bruker = saksbehandler) {
                    kall.journalpost.fullfor(
                        journalpostId = oppgave.journalpostId!!,
                        request =
                            JournalføringRequest(
                                stønadstype = Stønadstype.BARNETILSYN,
                                aksjon = JournalføringRequest.Journalføringsaksjon.OPPRETT_BEHANDLING,
                                årsak = JournalføringRequest.Journalføringsårsak.KLAGE,
                                oppgaveId = oppgave.id.toString(),
                                logiskeVedlegg = mapOf(dokumentInfoId to listOf(LogiskVedlegg(dokumentInfoId, "ny tittel"))),
                            ),
                    )
                }

            assertThat(journalpostId).isEqualTo(oppgave.journalpostId)

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
                        klageMottatt = LocalDate.now(),
                        behandlendeEnhet = oppgave.tildeltEnhetsnr!!,
                    ),
                )
            }

            verify(exactly = 1) { journalpostClient.ferdigstillJournalpost(journalpostId, oppgave.tildeltEnhetsnr!!, saksbehandler) }
            verify(exactly = 1) {
                journalpostClient.oppdaterLogiskeVedlegg(
                    dokumentInfoId = dokumentInfoId,
                    request = BulkOppdaterLogiskVedleggRequest(listOf("ny tittel")),
                )
            }
            verify(exactly = 1) { oppgaveClient.ferdigstillOppgave(oppgave.id, any()) }
        }
    }

    @Nested
    inner class HentJournalpost {
        @Test
        fun `hent journalpost som inneholder søknad daglig reise, kan kun opprette stønadstyper for daglig reise`() {
            val journalpost = journalpostMedStrukturertSøknad(DokumentBrevkode.DAGLIG_REISE)
            leggTilJournalpostMedSøknadIMock(journalpost, jsonMapper.writeValueAsBytes(søknadDagligReise()))
            every { ytelseClient.hentYtelser(any()) } returns ytelsePerioderDtoAAP()

            val journalpostResponse = kall.journalpost.journalpost(journalpost.journalpostId)

            assertThat(journalpostResponse.defaultStønadstype).isEqualTo(Stønadstype.DAGLIG_REISE_TSO)

            assertThat(journalpostResponse.valgbareStønadstyper).containsExactlyInAnyOrder(
                Stønadstype.DAGLIG_REISE_TSO,
                Stønadstype.DAGLIG_REISE_TSR,
            )
        }

        @Test
        fun `hent journalpost som inneholder søknad boutgifter, kan kunn opprette stønadstype boutgifter`() {
            val journalpost = journalpostMedStrukturertSøknad(DokumentBrevkode.BOUTGIFTER)
            leggTilJournalpostMedSøknadIMock(journalpost, jsonMapper.writeValueAsBytes(søknadBoutgifter()))

            val journalpostResponse = kall.journalpost.journalpost(journalpost.journalpostId)

            assertThat(journalpostResponse.valgbareStønadstyper).containsExactly(
                Stønadstype.BOUTGIFTER,
            )
        }
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
