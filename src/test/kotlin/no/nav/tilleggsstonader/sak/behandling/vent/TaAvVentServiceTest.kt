package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientConfig.Companion.MAPPE_ID_KLAR
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeMålgruppe
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.Optional

class TaAvVentServiceTest : IntegrationTest() {
    @Autowired
    lateinit var settPåVentService: SettPåVentService

    @Autowired
    lateinit var settPåVentRepository: SettPåVentRepository

    @Autowired
    lateinit var oppgaveService: OppgaveService

    @Autowired
    lateinit var behandlingshistorikkService: BehandlingshistorikkService

    @Autowired
    lateinit var taAvVentService: TaAvVentService

    @Autowired
    lateinit var vilkårperiodeService: VilkårperiodeService

    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak)
    var oppgaveId: Long? = null

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
        testWithBrukerContext(dummySaksbehandler) {
            settPåVentService.settPåVent(behandling.id, settPåVentDto.copy(beholdOppgave = true))
        }
    }

    val settPåVentDto =
        SettPåVentDto(
            årsaker = listOf(ÅrsakSettPåVent.ANNET),
            frist = osloDateNow().plusDays(3),
            kommentar = "ny beskrivelse",
        )

    @Test
    fun `skal ta av vent og fortsette behandling - uten dto`() {
        testWithBrukerContext(dummySaksbehandler) {
            taAvVentService.taAvVent(behandling.id)
        }

        validerTattAvVent(behandling.id)
        validerOppdatertOppgave(oppgaveId!!)
        validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
    }

    @Test
    fun `skal ta av vent og fortsette behandling - uten kommentar`() {
        testWithBrukerContext(dummySaksbehandler) {
            taAvVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = true, kommentar = null))
        }

        validerTattAvVent(behandling.id)
        validerOppdatertOppgave(oppgaveId!!)
        validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
    }

    @Test
    fun `skal ta av vent og fortsette behandling - med kommentar`() {
        testWithBrukerContext(dummySaksbehandler) {
            taAvVentService.taAvVent(
                behandlingId = behandling.id,
                taAvVentDto = TaAvVentDto(skalTilordnesRessurs = true, kommentar = "årsak av vent"),
            )
        }

        validerTattAvVent(behandling.id, kommentar = "årsak av vent")
        validerOppdatertOppgave(oppgaveId!!)
        validerHistorikkInnslag(behandling.id, skalHaMetadata = true)
    }

    @Test
    fun `skal ta av vent og markere oppgave som ufordelt - uten kommentar`() {
        testWithBrukerContext(dummySaksbehandler) {
            taAvVentService.taAvVent(behandling.id, TaAvVentDto(skalTilordnesRessurs = false, kommentar = null))
        }

        validerTattAvVent(behandling.id)
        validerOppdatertOppgave(oppgaveId!!)
        validerHistorikkInnslag(behandling.id, skalHaMetadata = false)
    }

    @Test
    fun `skal ta av vent og markere oppgave som ufordelt - med kommentar`() {
        testWithBrukerContext(dummySaksbehandler) {
            taAvVentService.taAvVent(
                behandlingId = behandling.id,
                taAvVentDto = TaAvVentDto(skalTilordnesRessurs = false, kommentar = "årsak av vent"),
            )
        }

        validerTattAvVent(behandling.id, kommentar = "årsak av vent")
        validerOppdatertOppgave(oppgaveId!!)
        validerHistorikkInnslag(behandling.id, skalHaMetadata = true)
    }

    @Test
    fun `skal feile hvis man ikke er eier av oppgaven`() {
        testWithBrukerContext {
            assertThatThrownBy {
                taAvVentService.taAvVent(
                    behandlingId = behandling.id,
                    taAvVentDto = TaAvVentDto(skalTilordnesRessurs = false, kommentar = "årsak av vent"),
                )
            }.hasMessageContaining("Kan ikke ta behandling av vent når man ikke er eier av oppgaven.")
        }
    }

    @Test
    fun `skal feile hvis behandlingen tas av vent to ganger etter hverandre`() {
        testWithBrukerContext(dummySaksbehandler) {
            taAvVentService.taAvVent(behandling.id)
            assertThatThrownBy {
                taAvVentService.taAvVent(behandling.id)
            }.hasMessageContaining("Behandlingen er allerede på vent")
        }
    }

    @Test
    fun `skal feile hvis det finnes en annen aktiv behandling på fagsaken`() {
        testWithBrukerContext(dummySaksbehandler) {
            testoppsettService.lagre(behandling(fagsak = fagsak))
            assertThatThrownBy {
                taAvVentService.taAvVent(behandling.id)
            }.hasMessageContaining("Det finnes allerede en aktiv behandling på denne fagsaken")
        }
    }

    @Test
    fun `skal nullstille behandling hvis en annen behandling på fagsaken har blitt iverksatt i mellomtiden`() {
        testWithBrukerContext(dummySaksbehandler) {
            // Lagre informasjon på behandlingen som skal nullstilles
            taAvVentService.taAvVent(behandling.id)
            vilkårperiodeService.opprettVilkårperiode(dummyVilkårperiodeMålgruppe(behandlingId = behandling.id))
            settPåVentService.settPåVent(behandling.id, settPåVentDto.copy(beholdOppgave = true))

            // Lag ny behandling som "sniker i køen" og blir iverksatt
            val behandlingSomSniker =
                behandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.UTREDES,
                    resultat = BehandlingResultat.INNVILGET,
                    // vedtakstidspunkt må være senere enn tidspunktet saken tas av vent for å "snike i køen"
                    vedtakstidspunkt = LocalDateTime.now().plusMinutes(10),
                )
            testoppsettService.lagre(behandlingSomSniker)
            vilkårperiodeService.opprettVilkårperiode(dummyVilkårperiodeAktivitet(behandlingId = behandlingSomSniker.id))
            testoppsettService.ferdigstillBehandling(behandlingSomSniker)

            // Ta den første behandlingen av vent og sjekk at den blir nullstilt og får ny forrigeIverksatteBehandlingId
            taAvVentService.taAvVent(behandling.id)

            val nullstilteVilkår = vilkårperiodeService.hentVilkårperioder(behandling.id)
            val nullstiltBehandling = testoppsettService.hentBehandling(behandling.id)
            assertThat(nullstiltBehandling.forrigeIverksatteBehandlingId).isEqualTo(behandlingSomSniker.id)
            assertThat(nullstilteVilkår.målgrupper).isEmpty()
            assertThat(nullstilteVilkår.aktiviteter).isNotEmpty()
        }
    }

    private fun validerTattAvVent(
        behandlingId: BehandlingId,
        kommentar: String? = null,
    ) {
        with(settPåVentRepository.findAll().single()) {
            assertThat(aktiv).isFalse()
            assertThat(taAvVentKommentar).isEqualTo(kommentar)
        }

        assertThat(testoppsettService.hentBehandling(behandlingId).status)
            .isEqualTo(BehandlingStatus.UTREDES)
    }

    private fun validerOppdatertOppgave(oppgaveId: Long) {
        with(oppgaveService.hentOppgave(oppgaveId)) {
            assertThat(tilordnetRessurs).isEqualTo(tilordnetRessurs)
            assertThat(beskrivelse).contains("Tatt av vent")
            assertThat(fristFerdigstillelse).isEqualTo(osloDateNow())
            assertThat(mappeId).isEqualTo(Optional.of(MAPPE_ID_KLAR))
        }
    }

    private fun validerHistorikkInnslag(
        behandlingId: BehandlingId,
        skalHaMetadata: Boolean,
    ) {
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
