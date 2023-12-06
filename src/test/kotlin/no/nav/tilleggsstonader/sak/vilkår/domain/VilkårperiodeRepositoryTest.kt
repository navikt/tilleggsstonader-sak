package no.nav.tilleggsstonader.sak.vilkår.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class VilkårperiodeRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Test
    internal fun `skal kunne lagre vilkårsperiode for målgruppe`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
        val vilkår = vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.MÅLGRUPPE_AAP))

        val vilkårperiode = vilkårperiodeRepository.insert(
            Vilkårperiode(
                vilkårId = vilkår.id,
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(5),
                type = MålgruppeType.AAP,
            ),
        )

        assertThat(vilkårperiodeRepository.findByIdOrThrow(vilkårperiode.vilkårId)).isEqualTo(vilkårperiode)
    }

    @Test
    internal fun `skal kunne lagre vilkårsperiode for aktivitet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
        val vilkår = vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.MÅLGRUPPE_AAP))

        val vilkårperiode = vilkårperiodeRepository.insert(
            Vilkårperiode(
                vilkårId = vilkår.id,
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(5),
                type = AktivitetType.TILTAK,
            ),
        )

        assertThat(vilkårperiodeRepository.findByIdOrThrow(vilkårperiode.vilkårId)).isEqualTo(vilkårperiode)
    }

    @Nested
    inner class FinnVilkårperioderForBehandling {
        @Test
        internal fun `skal finne alle vilkårsperioder for behandling`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
            val vilkårAAP =
                vilkårRepository.insert(vilkår(behandlingId = behandling.id, type = VilkårType.MÅLGRUPPE_AAP))
            val vilkårAAPFerdigAvklart = vilkårRepository.insert(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.MÅLGRUPPE_AAP_FERDIG_AVKLART,
                ),
            )

            val vilkårperiode1 = vilkårperiodeRepository.insert(
                Vilkårperiode(
                    vilkårId = vilkårAAP.id,
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(5),
                    type = MålgruppeType.AAP,
                ),
            )

            val vilkårperiode2 = vilkårperiodeRepository.insert(
                Vilkårperiode(
                    vilkårId = vilkårAAPFerdigAvklart.id,
                    fom = LocalDate.now(),
                    tom = LocalDate.now().plusDays(5),
                    type = MålgruppeType.AAP_FERDIG_AVKLART,
                ),
            )

            assertThat(vilkårperiodeRepository.finnVilkårperioderForBehandling(behandling.id))
                .containsExactlyInAnyOrder(vilkårperiode1, vilkårperiode2)
        }
    }
}
