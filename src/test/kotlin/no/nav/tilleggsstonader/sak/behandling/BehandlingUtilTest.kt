package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.skalNullstilleBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingUtilTest {

    @Nested
    inner class SkalNullstilleBehandling {

        val behandling = behandling(type = BehandlingType.REVURDERING)

        @Test
        fun `skal nullstille dersom revurderFra ikke finnes fra før`() {
            assertThat(skalNullstilleBehandling(behandling, nyRevurderFra = LocalDate.now())).isTrue
            assertThat(skalNullstilleBehandling(behandling, nyRevurderFra = LocalDate.now().minusDays(10))).isTrue
            assertThat(skalNullstilleBehandling(behandling, nyRevurderFra = LocalDate.now().plusDays(10))).isTrue
        }

        @Test
        fun `skal nullstille dersom revurderFra endres til etter tidligere revurderingsdato`() {
            val behandling = behandling.copy(revurderFra = LocalDate.now())
            assertThat(skalNullstilleBehandling(behandling, nyRevurderFra = LocalDate.now().plusDays(10))).isTrue
        }

        @Test
        fun `skal ikke nullstille dersom revurderFra endres til null ettersom man då viser alle perioder`() {
            val behandling = behandling.copy(revurderFra = LocalDate.now())
            assertThat(skalNullstilleBehandling(behandling, nyRevurderFra = null)).isFalse
        }

        @Test
        fun `skal ikke nullstille dersom revurderFra endres til før tidligere revurderingsdato ettersom man då viser flere perioder`() {
            val behandling = behandling.copy(revurderFra = LocalDate.now())
            assertThat(skalNullstilleBehandling(behandling, nyRevurderFra = LocalDate.now().minusDays(1))).isFalse
        }
    }
}
