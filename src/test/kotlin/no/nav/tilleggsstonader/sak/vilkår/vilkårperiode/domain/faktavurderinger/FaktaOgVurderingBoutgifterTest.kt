package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMottarSykepengerForFulltidsstilling
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingBoutgifterTest {
    @Nested
    inner class ResultatForTypeInngangsvilkår {
        @Test
        fun `resultatet skal ikke være oppfylt hvis ikke typen målgruppen gir rett på stønaden`() {
            listOf(IngenMålgruppeBoutgifter).forEach { faktaOgVurdering ->
                assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }

        @Test
        fun `resultatet skal ikke være oppfylt hvis ikke typen aktivitet gir rett på stønaden`() {
            listOf(IngenAktivitetBoutgifter).forEach { faktaOgVurdering ->
                assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }
    }

    @Nested
    inner class ResultatForInngangsvilkårMedVurderinger {
        val medlemskapIkkeVurdert = vurderingMedlemskap(svar = null)
        val medlemskapIkkeOppfylt = vurderingMedlemskap(svar = SvarJaNei.NEI)
        val medlemskapOppfylt = vurderingMedlemskap()

        val dekketAvAnnetRegelverkIkkeVurdert = vurderingDekketAvAnnetRegelverk(svar = null)
        val dekketAvAnnetRegelverkIkkeOppfylt = vurderingDekketAvAnnetRegelverk(svar = SvarJaNei.JA)
        val dekketAvAnnetRegelverkOppfylt = vurderingDekketAvAnnetRegelverk()

        val mottarSykepengerIkkeOppfylt = vurderingMottarSykepengerForFulltidsstilling(svar = SvarJaNei.JA)
        val mottarSykepengerOppfylt = vurderingMottarSykepengerForFulltidsstilling(SvarJaNei.NEI)

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og resten er oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneBoutgifter(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapIkkeVurdert,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
                            mottarSykepengerForFulltidsstilling = mottarSykepengerOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og resten er ikke oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneBoutgifter(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapIkkeOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeVurdert,
                            mottarSykepengerForFulltidsstilling = mottarSykepengerIkkeOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYLT hvis en vurdering er oppfylt og resten er ikke oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneBoutgifter(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeOppfylt,
                            mottarSykepengerForFulltidsstilling = mottarSykepengerIkkeOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneBoutgifter(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
                            mottarSykepengerForFulltidsstilling = mottarSykepengerOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis det ikke finnes noen vurderinger som trengs`() {
            val inngangsvilkår = OvergangssstønadBoutgifter

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }
}
