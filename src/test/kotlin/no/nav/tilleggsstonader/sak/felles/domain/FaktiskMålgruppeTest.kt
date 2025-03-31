package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktiskMålgruppeTest {
    @Test
    fun `prioritet skal være unik verdi per enum`() {
        val entriesMedPrioritet =
            FaktiskMålgruppe.entries.mapNotNull {
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
            FaktiskMålgruppe.entries
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
                FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                FaktiskMålgruppe.ENSLIG_FORSØRGER,
                FaktiskMålgruppe.GJENLEVENDE,
            )
    }

    @Nested
    inner class `skal mappe til rett type andel` {
        @Nested
        inner class `for tilsyn barn` {
            @Test
            fun `fra NEDSATT_ARBEIDSEVNE`() {
                assertThat(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE.tilTypeAndel(Stønadstype.BARNETILSYN)).isEqualTo(
                    TypeAndel.TILSYN_BARN_AAP,
                )
            }

            @Test
            fun `fra ENSLIG_FORSØRGER`() {
                assertThat(
                    FaktiskMålgruppe.ENSLIG_FORSØRGER.tilTypeAndel(Stønadstype.BARNETILSYN),
                ).isEqualTo(TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER)
            }

            @Test
            fun `fra GJENLEVENDE`() {
                assertThat(
                    FaktiskMålgruppe.GJENLEVENDE.tilTypeAndel(Stønadstype.BARNETILSYN),
                ).isEqualTo(TypeAndel.TILSYN_BARN_ETTERLATTE)
            }
        }

        @Nested
        inner class `for læremidler` {
            @Test
            fun `fra NEDSATT_ARBEIDSEVNE`() {
                assertThat(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE.tilTypeAndel(Stønadstype.LÆREMIDLER)).isEqualTo(
                    TypeAndel.LÆREMIDLER_AAP,
                )
            }

            @Test
            fun `fra ENSLIG_FORSØRGER`() {
                assertThat(
                    FaktiskMålgruppe.ENSLIG_FORSØRGER.tilTypeAndel(Stønadstype.LÆREMIDLER),
                ).isEqualTo(TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER)
            }

            @Test
            fun `fra GJENLEVENDE`() {
                assertThat(
                    FaktiskMålgruppe.GJENLEVENDE.tilTypeAndel(Stønadstype.LÆREMIDLER),
                ).isEqualTo(TypeAndel.LÆREMIDLER_ETTERLATTE)
            }
        }

        @Nested
        inner class `for boutgifter` {
            @Test
            fun `fra NEDSATT_ARBEIDSEVNE`() {
                assertThat(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE.tilTypeAndel(Stønadstype.BOUTGIFTER)).isEqualTo(
                    TypeAndel.BOUTGIFTER_AAP,
                )
            }

            @Test
            fun `fra ENSLIG_FORSØRGER`() {
                assertThat(
                    FaktiskMålgruppe.ENSLIG_FORSØRGER.tilTypeAndel(Stønadstype.BOUTGIFTER),
                ).isEqualTo(TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER)
            }

            @Test
            fun `fra GJENLEVENDE`() {
                assertThat(
                    FaktiskMålgruppe.GJENLEVENDE.tilTypeAndel(Stønadstype.BOUTGIFTER),
                ).isEqualTo(TypeAndel.BOUTGIFTER_ETTERLATTE)
            }
        }
    }
}
