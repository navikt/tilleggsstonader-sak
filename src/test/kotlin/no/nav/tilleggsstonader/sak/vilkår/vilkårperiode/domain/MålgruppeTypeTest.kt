package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
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
