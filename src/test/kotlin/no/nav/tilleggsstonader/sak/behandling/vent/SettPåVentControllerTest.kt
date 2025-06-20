package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
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
            frist = osloDateNow().plusDays(3),
            kommentar = "kommentar",
        )

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken(saksbehandler = dummySaksbehandler))
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
            testWithBrukerContext(dummySaksbehandler) {
                kallSettPåVentEndepunkt(behandling.id, settPåVentDto)
            }
        }

        @Test
        fun `retunerer OK når behandlingen kan tas av vent`() {
            val res =
                testWithBrukerContext(dummySaksbehandler) {
                    kallKanTaAvVentEndepunkt(behandling.id)
                }
            assertThat(res.body).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.OK))
        }

        @Test
        fun `retunerer MÅ_NULLSTILLE_BEHANDLING når en behandling har sneket i køen`() {
            val behandlingSomSniker =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                    vedtakstidspunkt = LocalDateTime.now(),
                )
            testoppsettService.lagre(behandlingSomSniker)

            val res = testWithBrukerContext(dummySaksbehandler) { kallKanTaAvVentEndepunkt(behandling.id) }
            assertThat(res.body).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.MÅ_NULLSTILLE_BEHANDLING))
        }

        @Test
        fun `retunerer ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAGKEN når det er annen aktiv behandling på fagsaken`() {
            val aktivBehandling =
                behandling(fagsak = fagsak, status = BehandlingStatus.UTREDES)
            testoppsettService.lagre(aktivBehandling)

            val res = kallKanTaAvVentEndepunkt(behandling.id)
            assertThat(res.body).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.ANNEN_AKTIV_BEHANDLING_PÅ_FAGSAKEN))
        }

        private fun kallKanTaAvVentEndepunkt(behandlingId: BehandlingId) =
            restTemplate.exchange<KanTaAvVentDto>(
                localhost("api/sett-pa-vent/$behandlingId/kan-ta-av-vent"),
                HttpMethod.GET,
                HttpEntity(null, headers),
            )
    }

    private fun kallSettPåVentEndepunkt(
        behandlingId: BehandlingId,
        settPåVentDto: SettPåVentDto,
    ) = restTemplate.exchange<StatusPåVentDto>(
        localhost("api/sett-pa-vent/$behandlingId"),
        HttpMethod.POST,
        HttpEntity(settPåVentDto, headers),
    )
}
