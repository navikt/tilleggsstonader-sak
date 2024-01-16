package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeDomainUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeDomainUtil.målgruppe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
    inner class FinnVilkårperioderForBehandling {
        @Test
        internal fun `skal finne alle vilkårsperioder for behandling`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

            val vilkårperiode1 = vilkårperiodeRepository.insert(målgruppe(behandlingId = behandling.id))
            val vilkårperiode2 =
                vilkårperiodeRepository.insert(målgruppe(behandlingId = behandling.id, type = MålgruppeType.UFØRETRYGD))

            assertThat(vilkårperiodeRepository.findByBehandlingId(behandling.id))
                .containsExactlyInAnyOrder(vilkårperiode1, vilkårperiode2)
        }
    }
}
