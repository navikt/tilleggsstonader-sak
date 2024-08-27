package no.nav.tilleggsstonader.sak.journalføring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariant
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalposttype
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingTestUtil
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.GjennbrukDataRevurderingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest
import no.nav.tilleggsstonader.sak.journalføring.dto.JournalføringRequest.Journalføringsaksjon
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class JournalføringServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val journalpostService = mockk<JournalpostService>()
    val søknadService = mockk<SøknadService>()
    val taskService = mockk<TaskService>()
    val barnService = mockk<BarnService>()
    val personService = mockk<PersonService>()
    val oppgaveService = mockk<OppgaveService>()
    val gjennbrukDataRevurderingService = mockk<GjennbrukDataRevurderingService>(relaxed = true)

    val journalføringService = JournalføringService(
        behandlingService,
        fagsakService,
        journalpostService,
        søknadService,
        taskService,
        barnService,
        TransactionHandler(),
        personService,
        oppgaveService,
        gjennbrukDataRevurderingService,
    )

    val enhet = ArbeidsfordelingTestUtil.ENHET_NASJONAL_NAY.enhetNr
    val personIdent = "123456789"
    val fagsak = fagsak(identer = setOf(PersonIdent(personIdent)))
    val journalpostId = "1"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = listOf(DokumentInfo("", brevkode = "1")),
        bruker = Bruker(personIdent, BrukerIdType.FNR),
        journalforendeEnhet = "123",
    )

    val taskSlot = slot<Task>()
    val nyAvsenderSlot = slot<AvsenderMottaker?>()

    @BeforeEach
    fun setUp() {
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { fagsakService.hentEllerOpprettFagsak(any(), any()) } returns fagsak

        justRun { behandlingService.leggTilBehandlingsjournalpost(any(), any(), any()) }
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()

        every { taskService.save(capture(taskSlot)) } returns mockk()
        every { personService.hentPersonIdenter(personIdent) } returns PdlIdenter(listOf(PdlIdent(personIdent, false)))
        justRun { oppgaveService.ferdigstillOppgave(any()) }
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost
        every { journalpostService.hentIdentFraJournalpost(any()) } returns personIdent
        justRun { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any(), any(), captureNullable(nyAvsenderSlot)) }
        every { søknadService.lagreSøknad(any(), any(), any()) } returns mockk()
    }

    @AfterEach
    fun tearDown() {
        taskSlot.clear()
        nyAvsenderSlot.clear()
    }

    @Test
    internal fun `skal ikke kunne journalføre hvis journalpostens bruker mangler`() {
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = null)

        assertThatThrownBy {
            journalføringService.journalførTilNyBehandling(
                journalpostId,
                personIdent,
                Stønadstype.BARNETILSYN,
                BehandlingÅrsak.SØKNAD,
                "oppgaveBeskrivelse",
                enhet,
            )
        }.hasMessageContaining("Journalposten mangler bruker")
    }

    @Test
    internal fun `skal kunne journalføre og opprette behandling`() {
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak

        every {
            behandlingService.opprettBehandling(
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        } returns behandling(fagsak = fagsak)
        every { journalpostService.hentSøknadFraJournalpost(any()) } returns mockk()

        journalføringService.journalførTilNyBehandling(
            journalpostId,
            personIdent,
            Stønadstype.BARNETILSYN,
            BehandlingÅrsak.SØKNAD,
            "beskrivelse",
            enhet,
        )

        verify(exactly = 1) {
            behandlingService.opprettBehandling(
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.SØKNAD,
            )
        }
        verify(exactly = 1) {
            journalpostService.oppdaterOgFerdigstillJournalpost(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }

        assertThat(taskSlot.captured.type).isEqualTo(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
    }

    @Test
    fun `skal oppdatere journalpost med avsender fra journalføringsrequest`() {
        val nyAvsender = JournalføringRequest.NyAvsender(true, "navn", personIdent)

        val journalføringRequest = JournalføringRequest(
            stønadstype = Stønadstype.BARNETILSYN,
            oppgaveId = "1",
            journalførendeEnhet = "123",
            årsak = JournalføringRequest.Journalføringsårsak.PAPIRSØKNAD,
            aksjon = Journalføringsaksjon.JOURNALFØR_PÅ_FAGSAK,
            nyAvsender = nyAvsender,
        )

        journalføringService.fullførJournalpost(journalføringRequest, journalpost)

        assertThat(nyAvsenderSlot.captured!!.navn).isEqualTo(nyAvsender.navn)
        assertThat(nyAvsenderSlot.captured!!.id).isEqualTo(nyAvsender.personIdent)
        assertThat(nyAvsenderSlot.captured!!.idType).isEqualTo(BrukerIdType.FNR)
    }

    @Nested
    inner class GjennbrukRevudering {

        val forrigeBehandling =
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
            )
        val nyBehandling = behandling(fagsak = fagsak)
        val barn1 = SøknadBarn(ident = "123456789", data = mockk())
        val barn2 = SøknadBarn(ident = "987654321", data = mockk())
        val eksisterendeBarn = listOf(barn1, barn2)

        @BeforeEach
        fun setUp() {
            every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(
                dokumenter = listOf(
                    DokumentInfo(
                        "",
                        brevkode = DokumentBrevkode.BARNETILSYN.verdi,
                        dokumentvarianter = listOf(
                            Dokumentvariant(
                                variantformat = Dokumentvariantformat.ORIGINAL,
                                null,
                                true,
                            ),
                        ),
                    ),
                ),
            )
            every { behandlingService.finnesBehandlingForFagsak(any()) } returns true
            every { behandlingService.opprettBehandling(any(), any(), any(), any()) } returns nyBehandling
            every { behandlingService.hentBehandlinger(any<UUID>()) } returns listOf(forrigeBehandling)
            every { journalpostService.hentSøknadFraJournalpost(any()) } returns mockk()
            every { barnService.finnBarnPåBehandling(nyBehandling.id) } returns eksisterendeBarn.map { it.tilBehandlingBarn() }
            every { søknadService.hentSøknadBarnetilsyn(nyBehandling.id) } returns mockk() {
                every { barn } returns setOf(barn1, barn2)
            }
            every { barnService.opprettBarn(any()) } returns mockk()
        }

        @Test
        fun `skal gjennbruke data fra tidligere behandling`() {
            journalføringService.journalførTilNyBehandling(
                journalpost.journalpostId,
                personIdent,
                Stønadstype.BARNETILSYN,
                BehandlingÅrsak.NYE_OPPLYSNINGER,
                "",
                "4462",
            )

            verify(exactly = 1) {
                gjennbrukDataRevurderingService.gjenbrukData(
                    nyBehandling,
                    forrigeBehandling.id,
                )
            }
        }

        @Test
        fun `skal ta med nye barn fra søknad`() {
            val barn3 = SøknadBarn(ident = "nyttBarn", data = mockk())

            every { søknadService.hentSøknadBarnetilsyn(nyBehandling.id) } returns mockk() {
                every { barn } returns setOf(barn1, barn2, barn3)
            }

            val barnSlot = slot<List<BehandlingBarn>>()
            every { barnService.opprettBarn(capture(barnSlot)) } returns mockk()

            journalføringService.journalførTilNyBehandling(
                journalpost.journalpostId,
                personIdent,
                Stønadstype.BARNETILSYN,
                BehandlingÅrsak.NYE_OPPLYSNINGER,
                "",
                "4462",
            )

            assertThat(barnSlot.captured).size().isEqualTo(1)
            assertThat(barnSlot.captured.first().ident).isEqualTo(barn3.ident)
        }

        private fun SøknadBarn.tilBehandlingBarn() = BehandlingBarn(
            ident = ident,
            behandlingId = nyBehandling.id,
        )
    }
}
