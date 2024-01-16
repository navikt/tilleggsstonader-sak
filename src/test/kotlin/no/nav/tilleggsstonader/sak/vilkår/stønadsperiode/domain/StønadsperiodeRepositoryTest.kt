package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class StønadsperiodeRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var stønadsperiodeRepository: StønadsperiodeRepository

    @Test
    internal fun `skal kunne lagre og hente stønadsperiode`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
        val stønadsperiode = stønadsperiodeRepository.insert(
            Stønadsperiode(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(5),
                behandlingId = behandling.id,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            ),
        )

        assertThat(stønadsperiodeRepository.findByIdOrThrow(stønadsperiode.id)).isEqualTo(stønadsperiode)
    }

    @Test
    internal fun `skal finne alle stønadsperioder for behandling`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val stønadsperiode1 = stønadsperiodeRepository.insert(
            Stønadsperiode(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(5),
                behandlingId = behandling.id,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            ),
        )

        val stønadsperiode2 = stønadsperiodeRepository.insert(
            Stønadsperiode(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(5),
                behandlingId = behandling.id,
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            ),
        )

        assertThat(stønadsperiodeRepository.findAllByBehandlingId(behandling.id))
            .containsExactlyInAnyOrder(stønadsperiode1, stønadsperiode2)
    }
}
