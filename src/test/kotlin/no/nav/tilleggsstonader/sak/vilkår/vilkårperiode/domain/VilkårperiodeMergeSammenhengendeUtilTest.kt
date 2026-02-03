package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetDagligReiseTsr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårperiodeMergeSammenhengendeUtilTest {
    @Nested
    inner class MergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet {
        @Test
        fun `skal returnere tom map når ingen vilkårperioder`() {
            val result = emptyList<Vilkårperiode>().mergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet()

            assertThat(result).isEmpty()
        }

        @Test
        fun `skal filtrere bort ikke-oppfylte vilkårperioder`() {
            val vilkårperioder =
                listOf(
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(type = AktivitetType.TILTAK),
                        resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                        typeAktivitet = TypeAktivitet.GRUPPEAMO,
                    ),
                )

            val result = vilkårperioder.mergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet()

            assertThat(result).isEmpty()
        }

        @Test
        fun `skal gruppere etter typeAktivitet og ikke slå sammen perioder med forskjellig typeAktivitet`() {
            val vilkårperioder =
                listOf(
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 15),
                        faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(type = AktivitetType.TILTAK),
                        resultat = ResultatVilkårperiode.OPPFYLT,
                        typeAktivitet = TypeAktivitet.GRUPPEAMO,
                    ),
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 16),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(type = AktivitetType.TILTAK),
                        resultat = ResultatVilkårperiode.OPPFYLT,
                        typeAktivitet = TypeAktivitet.ENKELAMO,
                    ),
                )

            val result = vilkårperioder.mergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet()

            assertThat(result).hasSize(2)
            assertThat(result[TypeAktivitet.GRUPPEAMO]).containsExactly(
                Datoperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 15),
                ),
            )
            assertThat(result[TypeAktivitet.ENKELAMO]).containsExactly(
                Datoperiode(
                    fom = LocalDate.of(2025, 1, 16),
                    tom = LocalDate.of(2025, 1, 31),
                ),
            )
        }

        @Test
        fun `skal slå sammen sammenhengende perioder med samme typeAktivitet`() {
            val vilkårperioder =
                listOf(
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 15),
                        faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(type = AktivitetType.TILTAK),
                        resultat = ResultatVilkårperiode.OPPFYLT,
                        typeAktivitet = TypeAktivitet.GRUPPEAMO,
                    ),
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 16),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(type = AktivitetType.TILTAK),
                        resultat = ResultatVilkårperiode.OPPFYLT,
                        typeAktivitet = TypeAktivitet.GRUPPEAMO,
                    ),
                )

            val result = vilkårperioder.mergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet()

            assertThat(result).hasSize(1)
            assertThat(result[TypeAktivitet.GRUPPEAMO]).containsExactly(
                Datoperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ),
            )
        }

        @Test
        fun `skal ikke slå sammen perioder med gap for samme typeAktivitet`() {
            val vilkårperioder =
                listOf(
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 15),
                        faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(type = AktivitetType.TILTAK),
                        resultat = ResultatVilkårperiode.OPPFYLT,
                        typeAktivitet = TypeAktivitet.ARBTREN,
                    ),
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 20),
                        tom = LocalDate.of(2025, 1, 31),
                        faktaOgVurdering = faktaOgVurderingAktivitetDagligReiseTsr(type = AktivitetType.TILTAK),
                        resultat = ResultatVilkårperiode.OPPFYLT,
                        typeAktivitet = TypeAktivitet.ARBTREN,
                    ),
                )

            val result = vilkårperioder.mergeSammenhengendeOppfylteAktiviteterMedLikTypeAktivitet()

            assertThat(result).hasSize(1)
            assertThat(result[TypeAktivitet.ARBTREN]).containsExactly(
                Datoperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 15),
                ),
                Datoperiode(
                    fom = LocalDate.of(2025, 1, 20),
                    tom = LocalDate.of(2025, 1, 31),
                ),
            )
        }
    }
}
