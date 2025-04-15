package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.sak.behandling.oppsummering.BehandlingOppsummeringUtil.filtrerOgDelFraRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingOppsummeringUtilTest {
    @Nested
    inner class KuttRevurderFraTest {
        val januar = målgruppe(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 31)).tilOppsummertVilkårperiode()
        val februar = målgruppe(fom = LocalDate.of(2025, 2, 1), tom = LocalDate.of(2025, 2, 28)).tilOppsummertVilkårperiode()
        val mars = målgruppe(fom = LocalDate.of(2025, 3, 1), tom = LocalDate.of(2025, 3, 31)).tilOppsummertVilkårperiode()

        @Test
        fun `skal returnere alle perioder dersom revurder fra er før tidligste startdato`() {
            val perioder =
                listOf(
                    februar,
                    mars,
                )

            val justertePerioder =
                perioder.filtrerOgDelFraRevurderFra(
                    revurderFra = LocalDate.of(2024, 1, 1),
                )

            assertThat(justertePerioder).isEqualTo(perioder)
        }

        @Test
        fun `skal ikke returnere perioder som er før revurder fra`() {
            val perioder =
                listOf(
                    januar,
                    februar,
                )

            val justertePerioder =
                perioder.filtrerOgDelFraRevurderFra(
                    revurderFra = LocalDate.of(2025, 3, 1),
                )

            assertThat(justertePerioder).isEmpty()
        }

        @Test
        fun `skal kutte perioder som overlapper med revurder fra`() {
            val perioder =
                listOf(
                    januar,
                )

            val revurderFra = LocalDate.of(2025, 1, 12)

            val justertePerioder =
                perioder.filtrerOgDelFraRevurderFra(
                    revurderFra = revurderFra,
                )

            assertThat(justertePerioder).isEqualTo(
                listOf(
                    januar.copy(fom = revurderFra),
                ),
            )
        }
    }
}
