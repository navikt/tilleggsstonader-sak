package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.util.EnumUtil.enumName
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaVurderingerJsonFilesUtil.tilTypeFaktaOgVurderingSuffix
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Denne må fylles på etterhvert som man legger til flere enums for andre stønader
 */
val alleEnumTyperFaktaOgVurdering: List<Pair<Stønadstype, TypeFaktaOgVurdering>> =
    listOf(
        Stønadstype.BARNETILSYN to AktivitetTilsynBarnType.entries,
        Stønadstype.BARNETILSYN to MålgruppeTilsynBarnType.entries,
        Stønadstype.LÆREMIDLER to AktivitetLæremidlerType.entries,
        Stønadstype.LÆREMIDLER to MålgruppeLæremidlerType.entries,
        Stønadstype.BOUTGIFTER to AktivitetBoutgifterType.entries,
        Stønadstype.BOUTGIFTER to MålgruppeBoutgifterType.entries,
        Stønadstype.DAGLIG_REISE_TSO to AktivitetDagligReiseTsoType.entries,
        Stønadstype.DAGLIG_REISE_TSO to MålgruppeDagligReiseTsoType.entries,
    ).flatMap { (stønadstype, enums) -> enums.map { stønadstype to it } }

class TypeFaktaOgVurderingTest {
    /**
     * Hvis det mangler noe, legg til der der og i [alleEnumTyperFaktaOgVurdering]
     */
    @Test
    fun `sjekk at alle har riktig navn`() {
        alleEnumTyperFaktaOgVurdering.forEach { (stønadstype, type) ->
            when (type) {
                is TypeFaktaOgVurderingTilsynBarn -> type.assertHarRiktigNavn(stønadstype)
                is TypeFaktaOgVurderingLæremidler -> type.assertHarRiktigNavn(stønadstype)
                is TypeFaktaOgVurderingBoutgifter -> type.assertHarRiktigNavn(stønadstype)
                is TypeFaktaOgVurderingDagligReiseTso -> type.assertHarRiktigNavn(stønadstype)
                is TypeFaktaOgVurderingDagligReiseTsr -> type.assertHarRiktigNavn(stønadstype)
            }
        }
    }

    @Test
    fun `sjekker at det feiler hvis man bruker feil stønadstype`() {
        assertThatThrownBy {
            alleEnumTyperFaktaOgVurdering.forEach { (_, type) ->
                if (type == AktivitetTilsynBarnType.UTDANNING_TILSYN_BARN) {
                    type.assertHarRiktigNavn(Stønadstype.LÆREMIDLER)
                }
                if (type == AktivitetLæremidlerType.UTDANNING_LÆREMIDLER) {
                    type.assertHarRiktigNavn(Stønadstype.BARNETILSYN)
                }
                if (type == AktivitetBoutgifterType.UTDANNING_BOUTGIFTER) {
                    type.assertHarRiktigNavn(Stønadstype.LÆREMIDLER)
                }
                if (type == AktivitetDagligReiseTsoType.UTDANNING_DAGLIG_REISE_TSO) {
                    type.assertHarRiktigNavn(Stønadstype.LÆREMIDLER)
                }
            }
        }.hasMessageContaining("but was: \"UTDANNING_TILSYN_BARN\"")
    }

    private fun TypeFaktaOgVurdering.assertHarRiktigNavn(stønadstype: Stønadstype) {
        assertThat(this.enumName())
            .isEqualTo("${this.vilkårperiodeType.enumName()}_${stønadstype.tilTypeFaktaOgVurderingSuffix()}")
    }
}
