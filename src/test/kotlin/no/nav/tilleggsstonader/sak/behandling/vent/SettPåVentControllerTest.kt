package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.kall.hentKanTaAvVent
import no.nav.tilleggsstonader.sak.kall.settPåVent
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class SettPåVentControllerTest : IntegrationTest() {
    @Autowired
    lateinit var oppgaveService: OppgaveService

    val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
    val behandling = behandling(fagsak = fagsak)
    val dummySaksbehandler = "dummySaksbehandler"
    val settPåVentDto =
        SettPåVentDto(
            årsaker = listOf(ÅrsakSettPåVent.ANNET),
            frist = LocalDate.now().plusDays(3),
            kommentar = "kommentar",
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)

        oppgaveService.opprettOppgave(
            behandlingId = behandling.id,
            oppgave = OpprettOppgave(Oppgavetype.BehandleSak, tilordnetNavIdent = dummySaksbehandler),
        )
    }

    @Nested
    inner class KanTaAvVent {
        @BeforeEach
        fun setUp() {
            medBrukercontext(bruker = dummySaksbehandler) {
                settPåVent(behandling.id, settPåVentDto)
            }
        }

        @Test
        fun `retunerer OK når behandlingen kan tas av vent`() {
            val res =
                medBrukercontext(bruker = dummySaksbehandler) {
                    hentKanTaAvVent(behandling.id)
                }
            assertThat(res).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.OK))
        }

        @Test
        fun `retunerer MÅ_NULLSTILLE_BEHANDLING når en behandling har sneket i køen`() {
            val behandlingSomSniker =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                    vedtakstidspunkt = LocalDateTime.now().plusDays(2),
                )
            testoppsettService.lagre(behandlingSomSniker)

            val res = medBrukercontext(bruker = dummySaksbehandler) { hentKanTaAvVent(behandling.id) }
            assertThat(res).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.MÅ_NULLSTILLE_BEHANDLING))
        }

        @Test
        fun `retunerer ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAGKEN når det er annen aktiv behandling på fagsaken`() {
            val aktivBehandling =
                behandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
            testoppsettService.lagre(aktivBehandling)

            val res = hentKanTaAvVent(behandling.id)
            assertThat(res).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAKEN))
        }
    }
}
