package no.nav.tilleggsstonader.sak.behandling.vent

import io.mockk.every
import io.mockk.mockkObject
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_KLAR
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_PÅ_VENT
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
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
import java.util.Optional
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class SettPåVentServiceTest : IntegrationTest() {

    @Autowired
    lateinit var settPåVentService: SettPåVentService

    @Autowired
    lateinit var settPåVentRepository: SettPåVentRepository

    @Autowired
    lateinit var oppgaveService: OppgaveService

    @Autowired
    lateinit var behandlingshistorikkService: BehandlingshistorikkService

    val behandling = behandling()
    var oppgaveId: Long? = null

    val settPåVentDto = SettPåVentDto(
        årsaker = listOf(ÅrsakSettPåVent.ANNET),
        frist = osloDateNow().plusDays(3),
        kommentar = "ny beskrivelse",
    )

    val oppdaterSettPåVentDto = OppdaterSettPåVentDto(
        årsaker = listOf(ÅrsakSettPåVent.ANTALL_DAGER_PÅ_TILTAK),
        frist = osloDateNow().plusDays(5),
        kommentar = "oppdatert beskrivelse",
        oppgaveVersjon = 1,
    )

    val dummySaksbehandler = "saksbehandler1"

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        oppgaveId = oppgaveService.opprettOppgave(
            behandling.id,
            OpprettOppgave(Oppgavetype.BehandleSak, tilordnetNavIdent = "123"),
        )
    }

    @Nested
    inner class SettPåVent {

        @Test
        fun `skal sette behandling på vent`() {
            mockkObject(SikkerhetContext) {
                every { SikkerhetContext.hentSaksbehandlerEllerSystembruker() } returns dummySaksbehandler

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
                    assertThat(mappeId?.getOrNull()).isEqualTo(MAPPE_ID_PÅ_VENT)
                }

                with(behandlingshistorikkService.finnSisteBehandlingshistorikk(behandling.id)) {
                    assertThat(utfall).isEqualTo(StegUtfall.SATT_PÅ_VENT)
                    assertThat(metadata).isNotNull()
                }
            }
        }

        @Test
        fun `skal feile hvis man prøver å sette behandling på vent når den allerede er på vent`() {
            testWithBrukerContext { settPåVentService.settPåVent(behandling.id, settPåVentDto) }
            assertThatThrownBy {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }.hasMessageContaining("Kan ikke sette behandling på vent når status=${BehandlingStatus.SATT_PÅ_VENT}")
        }
    }

    @Nested
    inner class OppdaterSettPåVent {
        @Test
        fun `skal kunne oppdatere settPåVent`() {
            mockkObject(SikkerhetContext) {
                every { SikkerhetContext.hentSaksbehandlerEllerSystembruker() } returns dummySaksbehandler
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
    }

    @Nested
    inner class TaAvVent {
        @BeforeEach
        fun setUp() {
            testWithBrukerContext { settPåVentService.settPåVent(behandling.id, settPåVentDto) }
        }

        @Test
        fun `skal ta av vent og fortsette behandling - uten dto`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, null)
            }

            validerTattAvVent(behandling.id)
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
        }

        @Test
        fun `skal ta av vent og fortsette behandling - uten kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = true, kommentar = null))
            }

            validerTattAvVent(behandling.id)
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
        }

        @Test
        fun `skal ta av vent og fortsette behandling - med kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = true, kommentar = "kommentar"))
            }

            validerTattAvVent(behandling.id, kommentar = "kommentar")
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = true)
        }

        @Test
        fun `skal ta av vent og markere oppgave som ufordelt - uten kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = false, kommentar = null))
            }

            validerTattAvVent(behandling.id)
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
        }

        @Test
        fun `skal ta av vent og markere oppgave som ufordelt - med kommentar`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = false, kommentar = "kommentar"))
            }

            validerTattAvVent(behandling.id, kommentar = "kommentar")
            validerOppdatertOppgave(oppgaveId!!, tilordnetRessurs = dummySaksbehandler)
            validerHistorikkInnslag(behandling.id, skalHaMetadata = true)
        }

        private fun validerTattAvVent(behandlingId: UUID, kommentar: String? = null) {
            with(settPåVentRepository.findAll().single()) {
                assertThat(aktiv).isFalse()
                assertThat(taAvVentKommentar).isEqualTo(kommentar)
            }

            assertThat(testoppsettService.hentBehandling(behandlingId).status)
                .isEqualTo(BehandlingStatus.UTREDES)
        }
        private fun validerOppdatertOppgave(oppgaveId: Long, tilordnetRessurs: String?) {
            with(oppgaveService.hentOppgave(oppgaveId)) {
                assertThat(tilordnetRessurs).isEqualTo(tilordnetRessurs)
                assertThat(beskrivelse).contains("Tatt av vent")
                assertThat(fristFerdigstillelse).isEqualTo(osloDateNow())
                assertThat(mappeId).isEqualTo(Optional.of(MAPPE_ID_KLAR))
            }
        }

        private fun validerHistorikkInnslag(behandlingId: UUID, skalHaMetadata: Boolean) {
            with(behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId)) {
                assertThat(utfall).isEqualTo(StegUtfall.TATT_AV_VENT)
                if (skalHaMetadata) {
                    assertThat(metadata).isNotNull()
                } else {
                    assertThat(metadata).isNull()
                }
            }
        }
    }
}
