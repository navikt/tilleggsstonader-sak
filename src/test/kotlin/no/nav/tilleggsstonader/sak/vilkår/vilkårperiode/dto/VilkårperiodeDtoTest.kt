package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.medlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class VilkårperiodeDtoTest {

    @Test
    fun `skal validere at periode er gyldig`() {
        assertThatThrownBy {
            målgruppe(
                fom = LocalDate.now(),
                tom = LocalDate.now().minusDays(1),
            ).tilDto()
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }

    @Nested
    inner class MappingAvVurdering {

        @ParameterizedTest
        @EnumSource(
            value = ResultatDelvilkårperiode::class,
            names = ["IKKE_AKTUELT"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal returnere delviljår hvis resultat != IKKE_AKTUELT`(resultat: ResultatDelvilkårperiode) {
            val målgruppe = målgruppe(
                delvilkår = DelvilkårMålgruppe(
                    medlemskap = DelvilkårVilkårperiode.Vurdering(
                        svar = null,
                        begrunnelse = null,
                        resultat = resultat,
                    ),
                ),
            ).tilDto()
            assertThat(målgruppe.medlemskap).isNotNull()
            assertThat(målgruppe.medlemskap?.svar).isNull()
        }

        @Test
        fun `skal returnere delvilkårsresultat IKKE_AKTUELT som vurdering=null`() {
            val målgruppe = målgruppe(
                delvilkår = DelvilkårMålgruppe(
                    medlemskap = DelvilkårVilkårperiode.Vurdering(
                        svar = null,
                        begrunnelse = null,
                        resultat = ResultatDelvilkårperiode.IKKE_AKTUELT,
                    ),
                ),
            ).tilDto()
            assertThat(målgruppe.medlemskap).isNull()
        }
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
