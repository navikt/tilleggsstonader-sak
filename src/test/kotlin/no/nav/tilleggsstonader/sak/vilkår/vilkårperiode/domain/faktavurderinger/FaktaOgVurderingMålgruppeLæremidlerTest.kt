package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMottarSykepengerForFulltidsstilling
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

        private val mottarSykepengerOppfylt = vurderingMottarSykepengerForFulltidsstilling(svar = SvarJaNei.NEI)
        private val mottarSykepengerIkkeOppfylt = vurderingMottarSykepengerForFulltidsstilling(svar = SvarJaNei.JA)

        @Test
        fun `resultat er IKKE_VURDERT hvis én vurdering ikke er vurdert og resten er oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingerNedsattArbeidsevneLæremidler(
                            medlemskap = medlemskapIkkeVurdert,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_VURDERT hvis én vurdering mangler vurdering og resten ikke er oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingerNedsattArbeidsevneLæremidler(
                            medlemskap = medlemskapIkkeOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeVurdert,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYLT hvis minst én er ikke oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingerNedsattArbeidsevneLæremidler(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneLæremidler(
                    vurderinger =
                        VurderingerNedsattArbeidsevneLæremidler(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
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
