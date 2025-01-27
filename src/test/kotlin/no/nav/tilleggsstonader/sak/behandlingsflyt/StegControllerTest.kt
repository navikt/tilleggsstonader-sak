package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.util.UUID

class StegControllerTest : IntegrationTest() {
    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal resette steg`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling(status = BehandlingStatus.UTREDES, steg = StegType.BEREGNE_YTELSE),
            )

        assertThat(behandling.steg).isEqualTo(StegType.BEREGNE_YTELSE)

        resetSteg(behandling.id, StegType.INNGANGSVILKÅR)

        assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.INNGANGSVILKÅR)
    }

    private fun resetSteg(
        behandlingId: BehandlingId,
        stegType: StegType,
    ) = restTemplate.exchange<UUID>(
        localhost("api/steg/behandling/$behandlingId/reset"),
        HttpMethod.POST,
        HttpEntity(StegController.ResetStegRequest(stegType), headers),
    )
}
