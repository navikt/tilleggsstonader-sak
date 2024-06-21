package no.nav.tilleggsstonader.sak.behandling.manuell

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.journalpost.Bruker
import no.nav.tilleggsstonader.kontrakter.journalpost.DokumentInfo
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OpprettBehandlingFraJournalpostServiceTest {

    val personService = mockk<PersonService>()
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val journalpostService = mockk<JournalpostService>()
    val taskService = mockk<TaskService>(relaxed = true)
    val tilgangService = mockk<TilgangService>(relaxed = true)
    val barnService = mockk<BarnService>()

    val service = OpprettBehandlingFraJournalpostService(
        personService = personService,
        fagsakService = fagsakService,
        behandlingService = behandlingService,
        journalpostService = journalpostService,
        taskService = taskService,
        tilgangService = tilgangService,
        barnService = barnService,
        unleashService = mockUnleashService(),
    )

    val ident = "13518741815"
    val identBarn = "02501961038"

    val opprettedeBarnSlot = slot<List<BehandlingBarn>>()

    val fagsak = fagsak()
    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        every { journalpostService.hentJournalpost("1") } returns
            journalpost(
                journalpostId = "1",
                bruker = Bruker(ident, BrukerIdType.FNR),
                dokumenter = listOf(DokumentInfo("2", brevkode = "NAV 11-12.15B")),
            )
        every { journalpostService.hentIdentFraJournalpost(any()) } returns ident
        every { journalpostService.hentDokument(any(), "2", any()) } returns
            FileUtil.readFile("fyllut-sendinn/søknad-1-barn.xml").toByteArray()

        every { personService.hentPersonIdenter(ident) } returns PdlIdenter(listOf(PdlIdent(ident, false)))
        every { personService.hentPersonMedBarn(ident) } returns
            SøkerMedBarn(ident, mockk(), barn = mapOf(identBarn to mockk()))

        every { fagsakService.finnFagsak(any(), Stønadstype.BARNETILSYN) } returns fagsak
        every { fagsakService.hentEllerOpprettFagsak(personIdent = ident, Stønadstype.BARNETILSYN) } returns fagsak
        every { behandlingService.hentBehandlinger(any<UUID>()) } returns emptyList()
        every { behandlingService.opprettBehandling(fagsak.id, any(), any(), any(), any()) } returns behandling
        justRun { behandlingService.leggTilBehandlingsjournalpost(any(), any(), any()) }
        every { barnService.opprettBarn(capture(opprettedeBarnSlot)) } answers { firstArg() }
    }

    @Test
    fun `skal opprette behandling med barn fra journalpost`() {
        service.opprettBehandlingFraJournalpost("1")

        with(opprettedeBarnSlot.captured.single()) {
            assertThat(this.ident).isEqualTo(identBarn)
            assertThat(this.behandlingId).isEqualTo(behandling.id)
        }
    }

    @Test
    fun `skal feile hvis det finnes behandlinger fra før`() {
        every { behandlingService.hentBehandlinger(any<UUID>()) } returns listOf(behandling())

        assertThatThrownBy {
            service.opprettBehandlingFraJournalpost("1")
        }.hasMessageContaining("Det finnes allerede en behandling på personen")
    }
}
