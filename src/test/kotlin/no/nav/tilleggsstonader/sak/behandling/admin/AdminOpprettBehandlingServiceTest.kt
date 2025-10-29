package no.nav.tilleggsstonader.sak.behandling.admin

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingOppgaveMetadata
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingRequest
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AdminOpprettBehandlingServiceTest {
    val personService = mockk<PersonService>()
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val opprettBehandlingService = mockk<OpprettBehandlingService>()
    val taskService = mockk<TaskService>(relaxed = true)
    val barnService = mockk<BarnService>()

    val service =
        AdminOpprettBehandlingService(
            personService = personService,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            taskService = taskService,
            barnService = barnService,
            unleashService = mockUnleashService(),
            opprettBehandlingService = opprettBehandlingService,
        )

    val ident = "13518741815"
    val identBarn = "02501961038"

    val opprettedeBarnSlot = slot<List<BehandlingBarn>>()

    val fagsak = fagsak()
    val behandling = behandling()

    val saksbehandler = "mrsaksbehandler"

    val forventetOppgaveMetadata =
        OpprettBehandlingOppgaveMetadata.OppgaveMetadata(
            tilordneSaksbehandler = saksbehandler,
            beskrivelse = "Manuelt opprettet sak fra journalpost. Skal saksbehandles i ny løsning.",
            prioritet = OppgavePrioritet.NORM,
        )

    @BeforeEach
    fun setUp() {
        every { personService.hentFolkeregisterIdenter(ident) } returns
            PdlIdenter(listOf(PdlIdent(ident, false, "FOLKEREGISTERIDENT")))
        every { personService.hentPersonMedBarn(ident) } returns
            SøkerMedBarn(ident, mockk(), barn = mapOf(identBarn to mockk()))

        every { fagsakService.finnFagsak(any(), any<Stønadstype>()) } returns fagsak
        every { fagsakService.hentEllerOpprettFagsak(personIdent = ident, any<Stønadstype>()) } returns fagsak
        every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns emptyList()
        every { opprettBehandlingService.opprettBehandling(any()) } returns behandling
        every { barnService.opprettBarn(capture(opprettedeBarnSlot)) } answers { firstArg() }
        BrukerContextUtil.mockBrukerContext()

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns saksbehandler
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `skal opprette behandling med barn`() {
        service.opprettFørstegangsbehandling(
            stønadstype = Stønadstype.BARNETILSYN,
            ident = ident,
            valgteBarn = setOf(identBarn),
            medBrev = true,
            kravMottatt = LocalDate.now(),
        )

        with(opprettedeBarnSlot.captured.single()) {
            assertThat(this.ident).isEqualTo(identBarn)
            assertThat(this.behandlingId).isEqualTo(behandling.id)
        }
        verify(exactly = 1) {
            opprettBehandlingService.opprettBehandling(
                OpprettBehandlingRequest(
                    fagsakId = fagsak.id,
                    behandlingsårsak = BehandlingÅrsak.MANUELT_OPPRETTET,
                    kravMottatt = LocalDate.now(),
                    oppgaveMetadata = forventetOppgaveMetadata,
                ),
            )
        }
    }

    @Test
    fun `skal opprette behandling uten brev`() {
        service.opprettFørstegangsbehandling(
            stønadstype = Stønadstype.BARNETILSYN,
            ident = ident,
            valgteBarn = setOf(identBarn),
            medBrev = false,
            kravMottatt = LocalDate.now(),
        )

        with(opprettedeBarnSlot.captured.single()) {
            assertThat(this.ident).isEqualTo(identBarn)
            assertThat(this.behandlingId).isEqualTo(behandling.id)
        }
        verify(exactly = 1) {
            opprettBehandlingService.opprettBehandling(
                OpprettBehandlingRequest(
                    fagsakId = fagsak.id,
                    behandlingsårsak = BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV,
                    kravMottatt = LocalDate.now(),
                    oppgaveMetadata = forventetOppgaveMetadata,
                ),
            )
        }
    }

    @Test
    fun `skal opprette behandling for læremidler`() {
        service.opprettFørstegangsbehandling(
            stønadstype = Stønadstype.LÆREMIDLER,
            ident = ident,
            valgteBarn = setOf(),
            medBrev = false,
            kravMottatt = LocalDate.now(),
        )

        assertThat(opprettedeBarnSlot.isCaptured).isFalse()
        verify(exactly = 1) {
            opprettBehandlingService.opprettBehandling(
                OpprettBehandlingRequest(
                    fagsakId = fagsak.id,
                    behandlingsårsak = BehandlingÅrsak.MANUELT_OPPRETTET_UTEN_BREV,
                    kravMottatt = LocalDate.now(),
                    oppgaveMetadata = forventetOppgaveMetadata,
                ),
            )
        }
    }

    @Test
    fun `skal feile hvis det finnes behandlinger fra før`() {
        every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns listOf(behandling())

        assertThatThrownBy {
            service.opprettFørstegangsbehandling(
                stønadstype = Stønadstype.BARNETILSYN,
                ident = ident,
                valgteBarn = setOf(identBarn),
                medBrev = true,
                kravMottatt = LocalDate.now(),
            )
        }.hasMessageContaining("Det finnes allerede en behandling på personen")
    }

    @Test
    fun `skal feile hvis barnen ikke finnes på personen`() {
        assertThatThrownBy {
            service.opprettFørstegangsbehandling(
                stønadstype = Stønadstype.BARNETILSYN,
                ident = ident,
                valgteBarn = setOf(identBarn, "annenIdent"),
                medBrev = true,
                kravMottatt = LocalDate.now(),
            )
        }.hasMessageContaining("Barn finnes ikke på person")
    }

    @Test
    fun `skal feile hvis stønadstype ikke forventer barn`() {
        assertThatThrownBy {
            service.opprettFørstegangsbehandling(
                stønadstype = Stønadstype.LÆREMIDLER,
                ident = ident,
                valgteBarn = setOf(identBarn, "annenIdent"),
                medBrev = true,
                kravMottatt = LocalDate.now(),
            )
        }.hasMessageContaining("skal ikke ha barn")
    }
}
