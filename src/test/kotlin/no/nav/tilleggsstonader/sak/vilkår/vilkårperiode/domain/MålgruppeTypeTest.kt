package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.kontrakter.felles.Hovedytelse
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MålgruppeTypeTest {
    @Test
    fun `søknadens hovedytelser mappes til nye målgrupper`() {
        assertThat(Hovedytelse.UNGDOMSPROGRAMMET.tilMålgruppeType())
            .isEqualTo(MålgruppeType.UNGDOMSPROGRAMMET)
        assertThat(Hovedytelse.AKTIVITETSPENGER.tilMålgruppeType())
            .isEqualTo(MålgruppeType.AKTIVITETSPENGER)
    }

    @Test
    fun `ungdomsprogrammet er tilgjengelig for stønadstyper med tiltaksøkonomi`() {
        assertThat(
            listOf(
                Stønadstype.DAGLIG_REISE_TSR,
                Stønadstype.REISE_TIL_SAMLING_TSR,
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
                Stønadstype.FLYTTING_TSR,
            ),
        ).allMatch { MålgruppeType.UNGDOMSPROGRAMMET.kanBrukesForStønad(it) }

        assertThat(MålgruppeType.UNGDOMSPROGRAMMET.kanBrukesForStønad(Stønadstype.BARNETILSYN)).isFalse
        assertThat(MålgruppeType.UNGDOMSPROGRAMMET.kanBrukesForStønad(Stønadstype.REISE_TIL_SAMLING_TSO)).isFalse
    }

    @Test
    fun `aktivitetspenger er tilgjengelig for TSO-stønadene med egen andelstype`() {
        assertThat(
            listOf(
                Stønadstype.BARNETILSYN,
                Stønadstype.LÆREMIDLER,
                Stønadstype.BOUTGIFTER,
                Stønadstype.DAGLIG_REISE_TSO,
                Stønadstype.REISE_TIL_SAMLING_TSO,
                Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
                Stønadstype.FLYTTING_TSO,
            ),
        ).allMatch { MålgruppeType.AKTIVITETSPENGER.kanBrukesForStønad(it) }

        assertThat(MålgruppeType.AKTIVITETSPENGER.kanBrukesForStønad(Stønadstype.DAGLIG_REISE_TSR)).isFalse
    }

    @Test
    fun `aktivitetspenger skal vurdere aldersvilkår`() {
        assertThat(MålgruppeType.AKTIVITETSPENGER.skalVurdereAldersvilkår()).isTrue
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
                            MålgruppeType.DAGPENGER -> FaktiskMålgruppe.ARBEIDSSØKER
                            MålgruppeType.OMSTILLINGSSTØNAD -> FaktiskMålgruppe.GJENLEVENDE
                            MålgruppeType.OVERGANGSSTØNAD -> FaktiskMålgruppe.ENSLIG_FORSØRGER
                            MålgruppeType.NEDSATT_ARBEIDSEVNE -> FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
                            MålgruppeType.UFØRETRYGD -> FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
                            MålgruppeType.SYKEPENGER_100_PROSENT -> null
                            MålgruppeType.INGEN_MÅLGRUPPE -> null
                            MålgruppeType.TILTAKSPENGER -> FaktiskMålgruppe.ARBEIDSSØKER
                            MålgruppeType.KVALIFISERINGSSTØNAD -> FaktiskMålgruppe.ARBEIDSSØKER
                            MålgruppeType.INNSATT_I_FENGSEL -> FaktiskMålgruppe.ARBEIDSSØKER
                            MålgruppeType.UNGDOMSPROGRAMMET -> FaktiskMålgruppe.ARBEIDSSØKER
                            MålgruppeType.AKTIVITETSPENGER -> FaktiskMålgruppe.AKTIVITETSPENGER
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
