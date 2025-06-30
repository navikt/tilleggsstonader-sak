package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.BehandlingshistorikkRepository
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.BehandlingshistorikkDto
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.Hendelse
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_PÅ_VENT
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
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
    lateinit var taAvVentService: TaAvVentService

    @Autowired
    lateinit var oppgaveService: OppgaveService

    @Autowired
    lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    lateinit var behandlingshistorikkRepository: BehandlingshistorikkRepository

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak)
    var oppgaveId: Long? = null

    val settPåVentDto =
        SettPåVentDto(
            årsaker = listOf(ÅrsakSettPåVent.ANNET),
            frist = LocalDate.now().plusDays(3),
            kommentar = "ny beskrivelse",
        )

    val oppdaterSettPåVentDto =
        OppdaterSettPåVentDto(
            årsaker = listOf(ÅrsakSettPåVent.ANTALL_DAGER_PÅ_TILTAK),
            frist = LocalDate.now().plusDays(5),
            kommentar = "oppdatert beskrivelse",
            oppgaveVersjon = 1,
        )

    val dummySaksbehandler = "saksbehandler1"

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling)
        oppgaveId =
            oppgaveService.opprettOppgave(
                behandling.id,
                OpprettOppgave(Oppgavetype.BehandleSak, tilordnetNavIdent = dummySaksbehandler),
            )
    }

    @Nested
    inner class SettPåVent {
        @Test
        fun `skal sette behandling på vent`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)

                assertThat(testoppsettService.hentBehandling(behandling.id).status)
                    .isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

                with(settPåVentService.hentStatusSettPåVent(behandling.id)) {
                    assertThat(årsaker).isEqualTo(settPåVentDto.årsaker)
                    assertThat(frist).isEqualTo(settPåVentDto.frist)
                    assertThat(kommentar).contains("ny beskrivelse")
                }

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(beskrivelse).contains("ny beskrivelse")
                    assertThat(fristFerdigstillelse).isEqualTo(settPåVentDto.frist)
                    assertThat(tilordnetRessurs).isNull()
                    assertThat(mappeId?.getOrNull()).isEqualTo(MAPPE_ID_PÅ_VENT)
                }

                val behandlingshistorikk =
                    behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidAsc(behandling.id)

                assertThat(behandlingshistorikk).hasSize(2)

                with(behandlingshistorikk[0]) {
                    assertThat(utfall).isEqualTo(StegUtfall.UTREDNING_PÅBEGYNT)
                    assertThat(metadata).isNull()
                }

                with(behandlingshistorikk[1]) {
                    assertThat(utfall).isEqualTo(StegUtfall.SATT_PÅ_VENT)
                    assertThat(metadata).isNotNull()
                }
            }
        }

        @Test
        fun `skal sette behandling på vent og fortsette beholde oppgaven`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto.copy(beholdOppgave = true))

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(tilordnetRessurs).isEqualTo(dummySaksbehandler)
                }
            }
        }

        @Test
        fun `skal feile hvis man ikke er eier av oppgaven`() {
            testWithBrukerContext {
                assertThatThrownBy {
                    settPåVentService.settPåVent(behandling.id, settPåVentDto)
                }.hasMessageContaining("Kan ikke sette behandling på vent når man ikke er eier av oppgaven.")
            }
        }

        @Test
        fun `skal feile hvis man prøver å sette behandling på vent når den allerede er på vent`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }
            assertThatThrownBy {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }.hasMessageContaining("Kan ikke gjøre endringer på denne behandlingen fordi den er satt på vent.")
        }
    }

    @Nested
    inner class OppdaterSettPåVent {
        @Test
        fun `skal kunne oppdatere settPåVent`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
                plukkOppgaven()
                settPåVentService.oppdaterSettPåVent(behandling.id, oppdaterSettPåVentDto.copy(oppgaveVersjon = 3))

                assertThat(testoppsettService.hentBehandling(behandling.id).status)
                    .isEqualTo(BehandlingStatus.SATT_PÅ_VENT)

                with(settPåVentService.hentStatusSettPåVent(behandling.id)) {
                    assertThat(årsaker).isEqualTo(oppdaterSettPåVentDto.årsaker)
                    assertThat(frist).isEqualTo(oppdaterSettPåVentDto.frist)
                    assertThat(kommentar).contains("oppdatert beskrivelse")
                }

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(beskrivelse).contains("oppdatert beskrivelse")
                    assertThat(fristFerdigstillelse).isEqualTo(oppdaterSettPåVentDto.frist)
                    assertThat(tilordnetRessurs).isNull()
                    assertThat(mappeId?.getOrNull()).isEqualTo(MAPPE_ID_PÅ_VENT)
                }
            }
        }

        @Test
        fun `skal sette behandling på vent og fortsette beholde oppgaven`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
                plukkOppgaven()
                val dto = oppdaterSettPåVentDto.copy(oppgaveVersjon = 3, beholdOppgave = true)
                settPåVentService.oppdaterSettPåVent(behandling.id, dto)

                with(oppgaveService.hentOppgave(oppgaveId!!)) {
                    assertThat(tilordnetRessurs).isEqualTo(dummySaksbehandler)
                }
            }
        }

        @Test
        fun `skal feile hvis man ikke er eier av oppgaven`() {
            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
            }
            testWithBrukerContext {
                assertThatThrownBy {
                    settPåVentService.oppdaterSettPåVent(behandling.id, oppdaterSettPåVentDto.copy(oppgaveVersjon = 3))
                }.hasMessageContaining("Kan ikke oppdatere behandling på vent når man ikke er eier av oppgaven.")
            }
        }
    }

    @Nested
    inner class Historikk {
        @Test
        fun `skal returnere kommentar fra historikk når behandlingen ikke ennå er sendt til iverksetting eller ferdigstilt`() {
            val saksbehandling = saksbehandling(behandling = behandling)
            val taAvVentDto = TaAvVentDto(skalTilordnesRessurs = false, kommentar = "tatt av")

            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto)
                plukkOppgaven()
                taAvVentService.taAvVent(behandling.id, taAvVentDto)
            }

            val historikk = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling)
            historikk.finnMetadata(Hendelse.SATT_PÅ_VENT).assertMetadataInneholderEksakt(
                mapOf(
                    "kommentarSettPåVent" to "ny beskrivelse",
                    "årsaker" to listOf(ÅrsakSettPåVent.ANNET.name),
                ),
            )
            historikk.finnMetadata(Hendelse.TATT_AV_VENT).assertMetadataInneholderEksakt(
                mapOf(
                    "kommentar" to "tatt av",
                ),
            )
        }

            /*
             * skal skjule kommentarer fra vent-hendelser fra dto når en behandling er ferdigstilt for å ikke vise intern
             * kommunikasjon for saksbehandler
             */
        @Test
        fun `skal skjule kommentarer fra vent-hendelser fra dto når en behandling er ferdigstilt`() {
            val saksbehandling = saksbehandling(behandling = behandling)
            val taAvVentDto = TaAvVentDto(skalTilordnesRessurs = false, kommentar = "tatt av")

            testWithBrukerContext(dummySaksbehandler) {
                settPåVentService.settPåVent(behandling.id, settPåVentDto.copy(beholdOppgave = true))
                taAvVentService.taAvVent(behandling.id, taAvVentDto)
            }

            val ferdigstiltBehandling = saksbehandling.copy(status = BehandlingStatus.FERDIGSTILT)
            val historikk = behandlingshistorikkService.finnHendelseshistorikk(ferdigstiltBehandling)
            historikk.finnMetadata(Hendelse.SATT_PÅ_VENT).assertMetadataInneholderEksakt(
                mapOf(
                    "årsaker" to listOf(ÅrsakSettPåVent.ANNET.name),
                ),
            )
            historikk.finnMetadata(Hendelse.TATT_AV_VENT).assertMetadataInneholderEksakt(emptyMap())
        }

        private fun List<BehandlingshistorikkDto>.finnMetadata(hendelse: Hendelse) = this.single { it.hendelse == hendelse }

        private fun BehandlingshistorikkDto.assertMetadataInneholderEksakt(map: Map<String, Any>) {
            assertThat(this.metadata).containsExactlyInAnyOrderEntriesOf(map)
        }
    }

    private fun plukkOppgaven() {
        val opppgave = oppgaveService.hentOppgave(oppgaveId!!)
        oppgaveService.fordelOppgave(oppgaveId!!, dummySaksbehandler, opppgave.versjon)
    }
}
