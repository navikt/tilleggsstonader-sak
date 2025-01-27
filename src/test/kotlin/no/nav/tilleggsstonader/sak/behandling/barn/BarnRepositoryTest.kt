package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.conversion.DbActionExecutionException

class BarnRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var barnRepository: BarnRepository

    @Test
    fun `skal kunne opprette barn og hente på behandlingId`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val behandlingBarn = barnRepository.insert(behandlingBarn(behandlingId = behandling.id))

        val behandlingBarnFraBehandlingId = barnRepository.findByBehandlingId(behandling.id)

        assertThat(behandlingBarn).isEqualTo(behandlingBarnFraBehandlingId.single())
    }

    @Test
    fun `kan ikke ha 2 barn med samme ident på en behandling`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        barnRepository.insert(behandlingBarn(behandlingId = behandling.id))

        assertThatThrownBy {
            barnRepository.insert(behandlingBarn(behandlingId = behandling.id))
        }.isInstanceOf(DbActionExecutionException::class.java)
            .rootCause()
            .hasMessageContaining(
                "duplicate key value violates unique constraint \"behandling_barn_behandling_id_person_ident_idx\"",
            )
    }
}
