package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.resetSteg
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StegControllerTest : IntegrationTest() {
    @Test
    fun `skal resette steg`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling(status = BehandlingStatus.UTREDES, steg = StegType.BEREGNE_YTELSE),
            )

        assertThat(behandling.steg).isEqualTo(StegType.BEREGNE_YTELSE)

        resetSteg(behandling.id, StegController.ResetStegRequest(StegType.INNGANGSVILKÅR))

        assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.INNGANGSVILKÅR)
    }
}
