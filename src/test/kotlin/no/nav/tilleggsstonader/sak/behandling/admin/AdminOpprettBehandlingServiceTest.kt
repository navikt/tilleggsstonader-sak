package no.nav.tilleggsstonader.sak.behandling.admin

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.opplysninger.dto.SøkerMedBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AdminOpprettBehandlingServiceTest {

    val personService = mockk<PersonService>()
    val fagsakService = mockk<FagsakService>()
    val behandlingService = mockk<BehandlingService>()
    val taskService = mockk<TaskService>(relaxed = true)
    val barnService = mockk<BarnService>()

    val service = AdminOpprettBehandlingService(
        personService = personService,
        fagsakService = fagsakService,
        behandlingService = behandlingService,
        taskService = taskService,
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
        every { personService.hentPersonIdenter(ident) } returns PdlIdenter(listOf(PdlIdent(ident, false)))
        every { personService.hentPersonMedBarn(ident) } returns
            SøkerMedBarn(ident, mockk(), barn = mapOf(identBarn to mockk()))

        every { fagsakService.finnFagsak(any(), Stønadstype.BARNETILSYN) } returns fagsak
        every { fagsakService.hentEllerOpprettFagsak(personIdent = ident, Stønadstype.BARNETILSYN) } returns fagsak
        every { behandlingService.hentBehandlinger(any<UUID>()) } returns emptyList()
        every { behandlingService.opprettBehandling(fagsak.id, any(), any(), any(), any()) } returns behandling
        every { barnService.opprettBarn(capture(opprettedeBarnSlot)) } answers { firstArg() }
    }

    @Test
    fun `skal opprette behandling med barn`() {
        service.opprettFørstegangsbehandling(ident, setOf(identBarn))

        with(opprettedeBarnSlot.captured.single()) {
            assertThat(this.ident).isEqualTo(identBarn)
            assertThat(this.behandlingId).isEqualTo(behandling.id)
        }
        verify(exactly = 1) {
            behandlingService.opprettBehandling(
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.MANUELT_OPPRETTET,
            )
        }
    }

    @Test
    fun `skal feile hvis det finnes behandlinger fra før`() {
        every { behandlingService.hentBehandlinger(any<UUID>()) } returns listOf(behandling())

        assertThatThrownBy {
            service.opprettFørstegangsbehandling(ident, setOf(identBarn))
        }.hasMessageContaining("Det finnes allerede en behandling på personen")
    }

    @Test
    fun `skal feile hvis barnen ikke finnes på personen`() {
        assertThatThrownBy {
            service.opprettFørstegangsbehandling(ident, setOf(identBarn, "annenIdent"))
        }.hasMessageContaining("Barn finnes ikke på person")
    }
}
