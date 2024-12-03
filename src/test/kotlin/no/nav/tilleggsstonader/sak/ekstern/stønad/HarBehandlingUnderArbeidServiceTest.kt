package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.ekstern.stønad.HarBehandlingUnderArbeidService.Companion.erSøknadUnderBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HarBehandlingUnderArbeidServiceTest {

    private val statuserUnderBehandling = listOf(
        BehandlingStatus.OPPRETTET,
        BehandlingStatus.UTREDES,
        BehandlingStatus.FATTER_VEDTAK,
        BehandlingStatus.SATT_PÅ_VENT,
    )

    @Test
    fun `Skal gi false for alle årsaker utenom SØKNAD`() {
        BehandlingÅrsak.entries.filterNot { it == BehandlingÅrsak.SØKNAD }.forEach {
            assertThat(
                erSøknadUnderBehandling(
                    årsak = it,
                    status = BehandlingStatus.OPPRETTET,
                ),
            ).isFalse()
        }
    }

    @Test
    fun `Skal gi false for status som er en status under arbeid`() {
        BehandlingStatus.entries.filterNot { it in statuserUnderBehandling }.forEach {
            assertThat(
                erSøknadUnderBehandling(
                    årsak = BehandlingÅrsak.SØKNAD,
                    status = it,
                ),
            ).isFalse()
        }
    }

    @Test
    fun `Skal gi true for årsak som er Søknad og behandling status som er opprettet`() {
        statuserUnderBehandling.forEach {
            assertThat(
                erSøknadUnderBehandling(
                    årsak = BehandlingÅrsak.SØKNAD,
                    status = it,
                ),
            ).isTrue()
        }
    }
}
