package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import org.assertj.core.api.Assertions.assertThat
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
}
