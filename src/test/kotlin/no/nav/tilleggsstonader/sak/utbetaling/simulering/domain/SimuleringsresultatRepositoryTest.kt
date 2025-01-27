package no.nav.tilleggsstonader.sak.utbetaling.simulering.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SimuleringsresultatRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var repository: SimuleringsresultatRepository

    @Test
    fun `skal kunne lagre og hente simulering`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
        val simuleringsresultat =
            repository.insert(
                Simuleringsresultat(
                    behandling.id,
                    data = SimuleringJson(emptyList(), SimuleringDetaljer("id", LocalDate.now(), 10, emptyList())),
                ),
            )
        assertThat(repository.findByIdOrThrow(behandling.id)).isEqualTo(simuleringsresultat)
    }
}
