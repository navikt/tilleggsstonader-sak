package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange

class SettPåVentControllerTest : IntegrationTest() {
    val fagsak = fagsak(stønadstype = Stønadstype.LÆREMIDLER)
    val behandling = behandling(fagsak = fagsak, steg = StegType.BEREGNE_YTELSE, status = BehandlingStatus.SATT_PÅ_VENT)

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
    }

    @Nested
    inner class KanTaAvVent {
        @Test
        fun `retunerer OK når behandlingen kan tas av vent`() {
            val res = kallKanTaAvVentEndepunkt(behandling.id)
            assertThat(res.body).isEqualTo(KanTaAvVentDto(resultat = KanTaAvVentStatus.OK))
        }

        @Test
        fun `retunerer MÅ_NULLSTILLE_BEHANDLING når man må nullstille behandling`() {
            val behandlingSomSniker =
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                )
            testoppsettService.lagre(behandlingSomSniker)

            val res = kallKanTaAvVentEndepunkt(behandling.id)
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
}
