package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class VilkårperiodeRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Test
    internal fun `skal kunne lagre vilkårsperiode for målgruppe`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val vilkårperiode = vilkårperiodeRepository.insert(målgruppe(behandlingId = behandling.id))

        assertThat(vilkårperiodeRepository.findByIdOrThrow(vilkårperiode.id)).isEqualTo(vilkårperiode)
    }

    @Test
    internal fun `skal kunne lagre vilkårsperiode for aktivitet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val vilkårperiode = vilkårperiodeRepository.insert(aktivitet(behandlingId = behandling.id))

        assertThat(vilkårperiodeRepository.findByIdOrThrow(vilkårperiode.id)).isEqualTo(vilkårperiode)
    }

    @Nested
    inner class FinnVilkårperioderDtoForBehandling {
        @Test
        internal fun `skal finne alle vilkårsperioder for behandling`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

            val vilkårperiode1 = vilkårperiodeRepository.insert(målgruppe(behandlingId = behandling.id))
            val vilkårperiode2 =
                vilkårperiodeRepository.insert(
                    målgruppe(
                        behandlingId = behandling.id,
                        faktaOgVurdering =
                            faktaOgVurderingMålgruppe(
                                type = MålgruppeType.UFØRETRYGD,
                            ),
                    ),
                )

            assertThat(vilkårperiodeRepository.findByBehandlingId(behandling.id))
                .containsExactlyInAnyOrder(vilkårperiode1, vilkårperiode2)
        }
    }

    @Nested
    inner class GitVersjon {
        @Test
        fun `skal returnere null hvis det ikke finnes en versjon`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val målgruppe =
                målgruppe(
                    behandlingId = behandling.id,
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppe(
                            type = MålgruppeType.UFØRETRYGD,
                        ),
                ).copy(gitVersjon = null)
            vilkårperiodeRepository.insert(målgruppe)
            assertThat(vilkårperiodeRepository.findByIdOrThrow(målgruppe.id).gitVersjon).isNull()
        }

        @Test
        fun `skal returnere versjon hvis det finnes en versjon`() {
            val gitVersjon = UUID.randomUUID().toString()
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val målgruppe =
                målgruppe(
                    behandlingId = behandling.id,
                    faktaOgVurdering =
                        faktaOgVurderingMålgruppe(
                            type = MålgruppeType.UFØRETRYGD,
                        ),
                ).copy(gitVersjon = gitVersjon)
            vilkårperiodeRepository.insert(målgruppe)
            assertThat(vilkårperiodeRepository.findByIdOrThrow(målgruppe.id).gitVersjon).isEqualTo(gitVersjon)
        }
    }
}
