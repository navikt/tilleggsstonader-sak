package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class BehandlingOppsummeringServiceTest : IntegrationTest() {
    @Autowired
    lateinit var behandlingOppsummeringService: BehandlingOppsummeringService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Test
    fun `skal returnere false på at det finnes data å oppsummere om behandling ikke inneholder noe data`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

        val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

        assertThat(behandlingOppsummering.finnesDataÅOppsummere).isFalse()
        assertThat(behandlingOppsummering.aktiviteter).isEmpty()
        assertThat(behandlingOppsummering.målgrupper).isEmpty()
        assertThat(behandlingOppsummering.vilkår).isEmpty()
    }

    @Nested
    inner class OppsummeringVilkårperioder {
        @Test
        fun `skal slå sammen sammenhengende perioder`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

            vilkårperiodeRepository.insertAll(
                listOf(
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 12),
                    ),
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 13),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.målgrupper).hasSize(1)
            assertThat(behandlingOppsummering.målgrupper[0].fom).isEqualTo(LocalDate.of(2025, 1, 1))
            assertThat(behandlingOppsummering.målgrupper[0].tom).isEqualTo(LocalDate.of(2025, 1, 31))
        }

        @Test
        fun `skal ikke slå sammen sammenhengende perioder med ulike typer eller resultat`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())

            vilkårperiodeRepository.insertAll(
                listOf(
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 12),
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                    ),
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 13),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                        resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                    ),
                    målgruppe(
                        behandlingId = behandling.id,
                        fom = LocalDate.of(2025, 1, 13),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.målgrupper).hasSize(3)
        }
    }

    @Nested
    inner class OppsummeringStønadsvilkår {
        @Test
        fun `skal slå sammen sammenhengende vilkår med like verdier`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
            val barn1 = BarnId.random()

            vilkårRepository.insertAll(
                listOf(
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 28),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.vilkår).hasSize(1)
            assertThat(behandlingOppsummering.vilkår[0].barnId).isEqualTo(barn1)
            assertThat(behandlingOppsummering.vilkår[0].vilkår).hasSize(1)
        }

        @Test
        fun `skal ikke slå sammen vilkår for to ulike barn`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling = behandling())
            val barn1 = BarnId.random()
            val barn2 = BarnId.random()

            vilkårRepository.insertAll(
                listOf(
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn1,
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                    vilkår(
                        behandlingId = behandling.id,
                        type = VilkårType.PASS_BARN,
                        barnId = barn2,
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 28),
                    ),
                ),
            )

            val behandlingOppsummering = behandlingOppsummeringService.hentBehandlingOppsummering(behandling.id)

            assertThat(behandlingOppsummering.finnesDataÅOppsummere).isTrue()
            assertThat(behandlingOppsummering.vilkår).hasSize(2)
        }
    }
}
