package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingMålgruppeLæremidlerTest {
    @Nested
    inner class UtledningAvResultatBasertPåType {
        @Test
        fun `resultatet skal ikke være oppfylt hvis ikke målgruppen gir rett på stønaden`() {
            listOf(IngenMålgruppeLæremidler).forEach { faktaOgVurdering ->
                assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }
    }

    @Nested
    inner class UtledningResultatBasertPåVurderinger {
        private val medlemskapIkkeVurdert = vurderingMedlemskap(svar = null)
        private val medlemskapIkkeOppfylt = vurderingMedlemskap(svar = SvarJaNei.NEI)
        private val medlemskapOppfylt = vurderingMedlemskap()

        private val dekketAvAnnetRegelverkIkkeVurdert = vurderingDekketAvAnnetRegelverk(svar = null)
        private val dekketAvAnnetRegelverkIkkeOppfylt = vurderingDekketAvAnnetRegelverk(svar = SvarJaNei.JA)
        private val dekketAvAnnetRegelverkOppfylt = vurderingDekketAvAnnetRegelverk()

        @Test
        fun `resultat er IKKE_VURDERT hvis én vurdering ikke er vurdert og én er oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapIkkeVurdert,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
                            aldersvilkår =
                                VurderingAldersVilkår(
                                    SvarJaNei.JA,
                                    inputFakta = "input-fakta",
                                ),
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_VURDERT hvis én vurdering mangler vurdering og én ikke er oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapIkkeOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeVurdert,
                            aldersvilkår =
                                VurderingAldersVilkår(
                                    SvarJaNei.JA,
                                    inputFakta = "input-fakta",
                                ),
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYKT hvis én vurdering er oppfylt og én er ikke oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeOppfylt,
                            aldersvilkår =
                                VurderingAldersVilkår(
                                    SvarJaNei.JA,
                                    inputFakta = "input-fakta",
                                ),
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
                            aldersvilkår =
                                VurderingAldersVilkår(
                                    SvarJaNei.JA,
                                    inputFakta = "input-fakta",
                                ),
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis det ikke finnes noen påkrevde vurderinger`() {
            val inngangsvilkår = OvergangssstønadLæremidler

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }
}
