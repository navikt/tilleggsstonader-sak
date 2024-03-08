package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_PÅ_VENT
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

class SettPåVentServiceTest : IntegrationTest() {

    @Autowired
    lateinit var settPåVentService: SettPåVentService

    @Autowired
    lateinit var settPåVentRepository: SettPåVentRepository

    @Autowired
    lateinit var oppgaveService: OppgaveService

    val behandling = behandling()
    var oppgaveId: Long? = null

    val settPåVentDto = SettPåVentDto(
        årsaker = listOf(ÅrsakSettPåVent.ANNET),
        frist = LocalDate.now().plusDays(3),
        kommentar = "ny beskrivelse",
    )

    val oppdaterSettPåVentDto = OppdaterSettPåVentDto(
        årsaker = listOf(ÅrsakSettPåVent.ANTALL_DAGER_PÅ_TILTAK),
        frist = LocalDate.now().plusDays(5),
        kommentar = "oppdatert beskrivelse",
        oppgaveVersjon = 1,
    )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        oppgaveId = oppgaveService.opprettOppgave(behandling.id, OpprettOppgave(Oppgavetype.BehandleSak, tilordnetNavIdent = "123"))
    }

    @Nested
    inner class SettPåVent {

        @Test
        fun `skal sette behandling på vent`() {
            settPåVentService.settPåVent(behandling.id, settPåVentDto)

            assertThat(testoppsettService.hentBehandling(behandling.id).status)
                .isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

            with(settPåVentService.hentStatusSettPåVent(behandling.id)) {
                assertThat(årsaker).isEqualTo(settPåVentDto.årsaker)
                assertThat(frist).isEqualTo(settPåVentDto.frist)
                assertThat(kommentar).contains("ny beskrivelse")
            }

            with(oppgaveService.hentOppgave(oppgaveId!!)) {
                assertThat(beskrivelse).doesNotContain("ny beskrivelse")
                assertThat(fristFerdigstillelse).isEqualTo(settPåVentDto.frist)
                assertThat(tilordnetRessurs).isNull()
                assertThat(mappeId?.getOrNull()).isEqualTo(MAPPE_ID_PÅ_VENT.toLong())
            }
        }

        @Test
        fun `skal feile hvis man prøver å sette behandling på vent når den allerede er på vent`() {
            settPåVentService.settPåVent(behandling.id, settPåVentDto)
            assertThatThrownBy {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }.hasMessageContaining("Kan ikke sette behandling på vent når status=${BehandlingStatus.SATT_PÅ_VENT}")
        }
    }

    @Nested
    inner class OppdaterSettPåVent {
        @Test
        fun `skal kunne oppdatere settPåVent`() {
            settPåVentService.settPåVent(behandling.id, settPåVentDto)
            settPåVentService.oppdaterSettPåVent(behandling.id, oppdaterSettPåVentDto.copy(oppgaveVersjon = 2))

            assertThat(testoppsettService.hentBehandling(behandling.id).status)
                .isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

            with(settPåVentService.hentStatusSettPåVent(behandling.id)) {
                assertThat(årsaker).isEqualTo(oppdaterSettPåVentDto.årsaker)
                assertThat(frist).isEqualTo(oppdaterSettPåVentDto.frist)
                assertThat(kommentar).contains("oppdatert beskrivelse")
            }
        }
    }

    @Nested
    inner class TaAvVent {

        val identSaksbehandler = "saksbehandler1"

        @Test
        fun `skal kunne ta behandling av vent`() {
            settPåVentService.settPåVent(behandling.id, settPåVentDto)

            testWithBrukerContext(identSaksbehandler) {
                settPåVentService.taAvVent(behandling.id)
            }

            assertThat(settPåVentRepository.findAll().single().aktiv)
                .isFalse()

            assertThat(testoppsettService.hentBehandling(behandling.id).status)
                .isEqualTo(BehandlingStatus.UTREDES)

            with(oppgaveService.hentOppgave(oppgaveId!!)) {
                assertThat(tilordnetRessurs).isEqualTo(identSaksbehandler)
                assertThat(beskrivelse).contains("Tatt av vent")
                assertThat(fristFerdigstillelse).isEqualTo(LocalDate.now())
                assertThat(mappeId).isEmpty()
            }
        }
    }
}
