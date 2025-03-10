package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingBoutgifterTest {
    @Nested
    inner class ResultatForTypeInngangsvilkår {
        // TODO kopiert fra tilsyn barn. Kommenter inn når implementerer målgruppe for boutgifter
//        @Test
//        fun `resultatet skal ikke være oppfylt hvis ikke typen målgruppen gir rett på stønaden`() {
//            listOf(IngenMålgruppeTilsynBarn, SykepengerTilsynBarn).forEach { faktaOgVurdering ->
//                assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
//            }
//        }

        @Test
        fun `resultatet skal ikke være oppfylt hvis ikke typen aktivitet gir rett på stønaden`() {
            listOf(IngenAktivitetBoutgifter).forEach { faktaOgVurdering ->
                assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }
    }

    // TODO kopiert fra tilsyn barn. Kommenter inn når implementerer målgruppe for boutgifter
//    @Nested
//    inner class ResultatForInngangsvilkårMedVurderinger {
//        val medlemskapIkkeVurdert = vurderingMedlemskap(svar = null)
//        val medlemskapIkkeOppfylt = vurderingMedlemskap(svar = SvarJaNei.NEI)
//        val medlemskapOppfylt = vurderingMedlemskap()
//
//        val dekketAvAnnetRegelverkIkkeVurdert = vurderingDekketAvAnnetRegelverk(svar = null)
//        val dekketAvAnnetRegelverkIkkeOppfylt = vurderingDekketAvAnnetRegelverk(svar = SvarJaNei.JA)
//        val dekketAvAnnetRegelverkOppfylt = vurderingDekketAvAnnetRegelverk()
//
//        @Test
//        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og en er oppfylt`() {
//            val inngangsvilkår =
//                NedsattArbeidsevneTilsynBarn(
//                    vurderinger =
//                        VurderingNedsattArbeidsevne(
//                            medlemskap = medlemskapIkkeVurdert,
//                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
//                        ),
//                )
//
//            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
//        }
//
//        @Test
//        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og en er ikke oppfylt`() {
//            val inngangsvilkår =
//                NedsattArbeidsevneTilsynBarn(
//                    vurderinger =
//                        VurderingNedsattArbeidsevne(
//                            medlemskap = medlemskapIkkeOppfylt,
//                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeVurdert,
//                        ),
//                )
//
//            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
//        }
//
//        @Test
//        fun `resultat er IKKE_OPPFYKT hvis en vurdering er oppfylt og en er ikke oppfylt`() {
//            val inngangsvilkår =
//                NedsattArbeidsevneTilsynBarn(
//                    vurderinger =
//                        VurderingNedsattArbeidsevne(
//                            medlemskap = medlemskapOppfylt,
//                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeOppfylt,
//                        ),
//                )
//
//            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
//        }
//
//        @Test
//        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
//            val inngangsvilkår =
//                NedsattArbeidsevneTilsynBarn(
//                    vurderinger =
//                        VurderingNedsattArbeidsevne(
//                            medlemskap = medlemskapOppfylt,
//                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
//                        ),
//                )
//
//            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
//        }
//
//        @Test
//        fun `resultat er OPPFYLT hvis det ikke finnes noen vurderinger som trengs`() {
//            val inngangsvilkår = OvergangssstønadTilsynBarn
//
//            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
//        }
//    }
}
