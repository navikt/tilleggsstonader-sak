package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SettPåVentRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var repository: SettPåVentRepository

    @Test
    fun `skal kunne lagre og hente settPåVent`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        repository.insert(settPåVent(behandling, aktiv = false))
        val aktiv = repository.insert(settPåVent(behandling, aktiv = true))
        repository.insert(settPåVent(behandling, aktiv = false))

        val settPåVent = repository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertThat(settPåVent).isEqualTo(aktiv)
    }

    @Test
    fun `skal ikke kunne ha 2 aktive på samme behandling`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        repository.insert(settPåVent(behandling))

        val exception =
            catchException {
                repository.insert(settPåVent(behandling))
            }
        assertThat(exception.cause).hasMessageContaining("duplicate key value violates unique constraint")
    }

    fun settPåVent(
        behandling: Behandling,
        aktiv: Boolean = true,
    ) = SettPåVent(
        behandlingId = behandling.id,
        årsaker = listOf(ÅrsakSettPåVent.ANNET),
        oppgaveId = 1,
        aktiv = aktiv,
        kommentar = "kommentar",
    )
}
