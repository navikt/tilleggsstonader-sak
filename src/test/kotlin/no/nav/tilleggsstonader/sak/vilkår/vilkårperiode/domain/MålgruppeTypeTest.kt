package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MålgruppeTypeTest {
    @Test
    fun `prioritet skal være unik verdi per enum`() {
        val entriesMedPrioritet =
            MålgruppeType.entries.mapNotNull {
                try {
                    it.prioritet()
                } catch (e: Exception) {
                    null
                }
            }
        assertThat(entriesMedPrioritet).hasSize(entriesMedPrioritet.distinct().size)
    }

    @Test
    fun `sorter målgrupper etter prioritet - høyest først`() {
        val entries =
            MålgruppeType.entries
                .mapNotNull {
                    try {
                        it to it.prioritet()
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.second }
                .map { it.first }

        assertThat(entries)
            .containsExactly(
                MålgruppeType.AAP,
                MålgruppeType.NEDSATT_ARBEIDSEVNE,
                MålgruppeType.UFØRETRYGD,
                MålgruppeType.OVERGANGSSTØNAD,
                MålgruppeType.OMSTILLINGSSTØNAD,
            )
    }

    @Nested
    inner class `skal mappe til rett type andel` {
        @Nested
        inner class `for tilsyn barn` {
            @Test
            fun `fra AAP`() {
                assertThat(MålgruppeType.AAP.tilTypeAndel(Stønadstype.BARNETILSYN)).isEqualTo(TypeAndel.TILSYN_BARN_AAP)
            }

            @Test
            fun `fra UFØRETRYGD`() {
                assertThat(MålgruppeType.UFØRETRYGD.tilTypeAndel(Stønadstype.BARNETILSYN)).isEqualTo(TypeAndel.TILSYN_BARN_AAP)
            }

            @Test
            fun `fra NEDSATT_ARBEIDSEVNE`() {
                assertThat(MålgruppeType.NEDSATT_ARBEIDSEVNE.tilTypeAndel(Stønadstype.BARNETILSYN)).isEqualTo(TypeAndel.TILSYN_BARN_AAP)
            }

            @Test
            fun `fra OVERGANGSSTØNAD`() {
                assertThat(
                    MålgruppeType.OVERGANGSSTØNAD.tilTypeAndel(Stønadstype.BARNETILSYN),
                ).isEqualTo(TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER)
            }

            @Test
            fun `fra OMSTILLINGSSTØNAD`() {
                assertThat(
                    MålgruppeType.OMSTILLINGSSTØNAD.tilTypeAndel(Stønadstype.BARNETILSYN),
                ).isEqualTo(TypeAndel.TILSYN_BARN_ETTERLATTE)
            }

            @Test
            fun `fra SYKEPENGER_100_PROSENT`() {
                assertThatThrownBy {
                    MålgruppeType.SYKEPENGER_100_PROSENT.tilTypeAndel(Stønadstype.BARNETILSYN)
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessageContaining("Kan ikke opprette andel tilkjent ytelse for målgruppe SYKEPENGER_100_PROSENT")
            }
        }

        @Nested
        inner class `for læremidler` {
            @Test
            fun `fra AAP`() {
                assertThat(MålgruppeType.AAP.tilTypeAndel(Stønadstype.LÆREMIDLER)).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            }

            @Test
            fun `fra UFØRETRYGD`() {
                assertThat(MålgruppeType.UFØRETRYGD.tilTypeAndel(Stønadstype.LÆREMIDLER)).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            }

            @Test
            fun `fra NEDSATT_ARBEIDSEVNE`() {
                assertThat(MålgruppeType.NEDSATT_ARBEIDSEVNE.tilTypeAndel(Stønadstype.LÆREMIDLER)).isEqualTo(TypeAndel.LÆREMIDLER_AAP)
            }

            @Test
            fun `fra OVERGANGSSTØNAD`() {
                assertThat(
                    MålgruppeType.OVERGANGSSTØNAD.tilTypeAndel(Stønadstype.LÆREMIDLER),
                ).isEqualTo(TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER)
            }

            @Test
            fun `fra OMSTILLINGSSTØNAD`() {
                assertThat(
                    MålgruppeType.OMSTILLINGSSTØNAD.tilTypeAndel(Stønadstype.LÆREMIDLER),
                ).isEqualTo(TypeAndel.LÆREMIDLER_ETTERLATTE)
            }

            @Test
            fun `fra SYKEPENGER_100_PROSENT`() {
                assertThatThrownBy {
                    MålgruppeType.SYKEPENGER_100_PROSENT.tilTypeAndel(Stønadstype.LÆREMIDLER)
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessageContaining("Kan ikke opprette andel tilkjent ytelse for målgruppe SYKEPENGER_100_PROSENT")
            }
        }
    }

    @Nested
    inner class MappingTilFaktiskMålgruppe {
        @Test
        fun `skal mappe verdier til faktiskMålgruppe`() {
            val mappings =
                MålgruppeType.entries.map {
                    it to
                        when (it) {
                            MålgruppeType.AAP -> FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
                            MålgruppeType.DAGPENGER -> null
                            MålgruppeType.OMSTILLINGSSTØNAD -> FaktiskMålgruppe.GJENLEVENDE
                            MålgruppeType.OVERGANGSSTØNAD -> FaktiskMålgruppe.ENSLIG_FORSØRGER
                            MålgruppeType.NEDSATT_ARBEIDSEVNE -> FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
                            MålgruppeType.UFØRETRYGD -> FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
                            MålgruppeType.SYKEPENGER_100_PROSENT -> null
                            MålgruppeType.INGEN_MÅLGRUPPE -> null
                        }
                }

            mappings.filter { it.second != null }.forEach {
                assertThat(it.first.faktiskMålgruppe()).isEqualTo(it.second!!)
            }

            mappings.filter { it.second == null }.forEach {
                assertThatThrownBy {
                    it.first.faktiskMålgruppe()
                }.hasMessageContaining("Mangler faktisk målgruppe")
            }
        }
    }
}
