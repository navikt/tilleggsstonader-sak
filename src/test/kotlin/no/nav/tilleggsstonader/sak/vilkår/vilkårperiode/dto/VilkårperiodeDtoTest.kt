package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.dekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeExtensions.medlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode.IKKE_OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode.OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.UtdanningLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingMedlemskap
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
    inner class MappingAvVurdering {

        @Test
        fun `skal mappe vurdering fra vilkår`() {
            val målgruppe = målgruppe(
                begrunnelse = "begrunnelse",
                faktaOgVurdering = faktaOgVurderingMålgruppe(
                    type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                    medlemskap = vurderingMedlemskap(svar = SvarJaNei.JA),
                    dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(svar = SvarJaNei.JA),
                ),
            ).tilDto()

            assertThat(målgruppe.medlemskap).isNotNull
            assertThat(målgruppe.medlemskap?.svar).isEqualTo(SvarJaNei.JA)
            assertThat(målgruppe.medlemskap?.resultat).isEqualTo(OPPFYLT)
            assertThat(målgruppe.dekketAvAnnetRegelverk).isNotNull
            assertThat(målgruppe.dekketAvAnnetRegelverk?.svar).isEqualTo(SvarJaNei.JA)
            assertThat(målgruppe.dekketAvAnnetRegelverk?.resultat).isEqualTo(IKKE_OPPFYLT)
        }
    }

    @Nested
    inner class MappingAvFaktaOgVurderinger {

        @Test
        fun `mapper ut faktaOgVurderinger for målgruppe`() {
            val målgruppe = målgruppe(
                begrunnelse = "Begrunnelse",
                faktaOgVurdering = faktaOgVurderingMålgruppe(
                    type = MålgruppeType.NEDSATT_ARBEIDSEVNE,
                    medlemskap = VurderingMedlemskap(svar = SvarJaNei.JA),
                    dekketAvAnnetRegelverk = VurderingDekketAvAnnetRegelverk(svar = SvarJaNei.JA),
                ),
            ).tilDto()

            assertThat(målgruppe.faktaOgVurderinger).isEqualTo(
                MålgruppeFaktaOgVurderingerDto(
                    medlemskap = VurderingDto(svar = SvarJaNei.JA, resultat = OPPFYLT),
                    utgifterDekketAvAnnetRegelverk = VurderingDto(SvarJaNei.JA, resultat = IKKE_OPPFYLT),
                ),
            )
        }

        @Test
        fun `mapper ut faktaOgVurderinger for tiltak tilsyn barn`() {
            val tiltakTilsynBarn = VilkårperiodeTestUtil.aktivitet(
                faktaOgVurdering = faktaOgVurderingAktivitet(
                    type = AktivitetType.TILTAK,
                    aktivitetsdager = 3,
                    lønnet = VurderingLønnet(svar = SvarJaNei.NEI),
                ),
            ).tilDto()

            assertThat(tiltakTilsynBarn.faktaOgVurderinger).isEqualTo(
                AktivitetBarnetilsynFaktaOgVurderingerDto(
                    aktivitetsdager = 3,
                    lønnet = VurderingDto(SvarJaNei.NEI, resultat = OPPFYLT),
                ),
            )
        }

        @Test
        fun `mapper ut faktaOgVurderinger for utdanning læremidler`() {
            val utdanningLæremidler = VilkårperiodeTestUtil.aktivitet(
                faktaOgVurdering = UtdanningLæremidler(
                    fakta = FaktaAktivitetLæremidler(prosent = 60),
                ),
            ).tilDto()

            assertThat(utdanningLæremidler.faktaOgVurderinger).isEqualTo(
                AktivitetLæremidlerFaktaOgVurderingerDto(prosent = 60),
            )
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
