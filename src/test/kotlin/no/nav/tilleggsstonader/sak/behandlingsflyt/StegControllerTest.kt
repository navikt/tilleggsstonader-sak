package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StegControllerTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal resette steg`() {
        val behandling =
            testoppsettService.opprettBehandlingMedFagsak(
                behandling(status = BehandlingStatus.UTREDES, steg = StegType.BEREGNE_YTELSE),
            )
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        assertThat(behandling.steg).isEqualTo(StegType.BEREGNE_YTELSE)

        kall.steg.reset(behandling.id, StegController.ResetStegRequest(StegType.INNGANGSVILKÅR))

        assertThat(testoppsettService.hentBehandling(behandling.id).steg).isEqualTo(StegType.INNGANGSVILKÅR)
    }
}
