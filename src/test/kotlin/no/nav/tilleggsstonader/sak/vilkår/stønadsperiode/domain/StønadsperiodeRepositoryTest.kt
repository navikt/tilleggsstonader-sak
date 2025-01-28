package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class StønadsperiodeRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var stønadsperiodeRepository: StønadsperiodeRepository

    @Test
    internal fun `skal kunne lagre og hente stønadsperiode`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
        val stønadsperiode =
            stønadsperiodeRepository.insert(
                Stønadsperiode(
                    fom = osloDateNow(),
                    tom = osloDateNow().plusDays(5),
                    behandlingId = behandling.id,
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                    status = StønadsperiodeStatus.NY,
                ),
            )

        assertThat(stønadsperiodeRepository.findByIdOrThrow(stønadsperiode.id)).isEqualTo(stønadsperiode)
    }

    @Test
    internal fun `skal finne alle stønadsperioder for behandling`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val stønadsperiode1 =
            stønadsperiodeRepository.insert(
                Stønadsperiode(
                    fom = osloDateNow(),
                    tom = osloDateNow().plusDays(5),
                    behandlingId = behandling.id,
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                    status = StønadsperiodeStatus.NY,
                ),
            )

        val stønadsperiode2 =
            stønadsperiodeRepository.insert(
                Stønadsperiode(
                    fom = osloDateNow(),
                    tom = osloDateNow().plusDays(5),
                    behandlingId = behandling.id,
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                    status = StønadsperiodeStatus.NY,
                ),
            )

        assertThat(stønadsperiodeRepository.findAllByBehandlingId(behandling.id))
            .containsExactlyInAnyOrder(stønadsperiode1, stønadsperiode2)
    }
}
