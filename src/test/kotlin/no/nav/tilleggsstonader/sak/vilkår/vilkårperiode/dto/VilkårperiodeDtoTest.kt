package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårperiodeDtoTest {

    @Test
    fun `skal validere at periode er gyldig`() {
        assertThatThrownBy {
            målgruppe(
                fom = osloDateNow(),
                tom = osloDateNow().minusDays(1),
            ).tilDto()
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }

    @Nested
    inner class MergeSammenhengendeOgOppfylteVilkårperioder {

        @Test
        fun `overlapper med lik fom-dato`() {
            val perioder = listOf(
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 8),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 2),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
            )

            assertThat(perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!.first()).isEqualTo(
                Datoperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10)),
            )
        }

        @Test
        fun `sammenhengende uten overlapp`() {
            val perioder = listOf(
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 5),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
            )

            assertThat(perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!.first()).isEqualTo(
                Datoperiode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10)),
            )
        }

        @Test
        fun `to sammenhengende grupper`() {
            val perioder = listOf(
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 5),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 12),
                    tom = LocalDate.of(2023, 1, 20),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 16),
                    tom = LocalDate.of(2023, 1, 24),
                ).tilDto(),
            )

            val mergetPerioder = perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!

            assertThat(mergetPerioder.size).isEqualTo(2)
            assertThat(mergetPerioder[0]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                ),
            )
            assertThat(mergetPerioder[1]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 12),
                    tom = LocalDate.of(2023, 1, 24),
                ),
            )
        }

        @Test
        fun `skal fjerne ikke oppfylt periode`() {
            val perioder = listOf(
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 3),
                    tom = LocalDate.of(2023, 1, 10),
                    resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 20),
                ).tilDto(),
            )

            val mergetPerioder = perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!

            assertThat(mergetPerioder.size).isEqualTo(2)
            assertThat(mergetPerioder[0]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 4),
                ),
            )
            assertThat(mergetPerioder[1]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 11),
                    tom = LocalDate.of(2023, 1, 20),
                ),
            )
        }

        @Test
        fun `fullstendig omsluttende`() {
            val perioder = listOf(
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                ).tilDto(),
                VilkårperiodeTestUtil.aktivitet(
                    fom = LocalDate.of(2023, 1, 3),
                    tom = LocalDate.of(2023, 1, 8),
                ).tilDto(),
            )

            val mergetPerioder = perioder.mergeSammenhengendeOppfylteVilkårperioder()[AktivitetType.TILTAK]!!

            assertThat(mergetPerioder.size).isEqualTo(1)
            assertThat(mergetPerioder[0]).isEqualTo(
                Datoperiode(
                    fom = LocalDate.of(2023, 1, 1),
                    tom = LocalDate.of(2023, 1, 10),
                ),
            )
        }
    }
}
