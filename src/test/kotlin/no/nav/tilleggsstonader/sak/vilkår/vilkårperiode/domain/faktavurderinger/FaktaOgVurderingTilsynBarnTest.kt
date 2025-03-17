package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingTilsynBarnTest {
    @Nested
    inner class ResultatForTypeInngangsvilkår {
        @Test
        fun `resultatet skal ikke være oppfylt hvis ikke typen målgruppen gir rett på stønaden`() {
            listOf(IngenMålgruppeTilsynBarn, SykepengerTilsynBarn).forEach { faktaOgVurdering ->
                assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        }

        @Test
        fun `resultatet skal ikke være oppfylt hvis ikke typen aktivitet gir rett på stønaden`() {
            listOf(IngenAktivitetTilsynBarn).forEach { faktaOgVurdering ->
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

        val aldersvilkårOppfylt = vurderingAldersVilkår()

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og en er oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneTilsynBarn(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapIkkeVurdert,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
                            aldersvilkår = aldersvilkårOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og en er ikke oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneTilsynBarn(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapIkkeOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeVurdert,
                            aldersvilkår = aldersvilkårOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYKT hvis en vurdering er oppfylt og en er ikke oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneTilsynBarn(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkIkkeOppfylt,
                            aldersvilkår = aldersvilkårOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
            val inngangsvilkår =
                NedsattArbeidsevneTilsynBarn(
                    vurderinger =
                        VurderingNedsattArbeidsevne(
                            medlemskap = medlemskapOppfylt,
                            dekketAvAnnetRegelverk = dekketAvAnnetRegelverkOppfylt,
                            aldersvilkår = aldersvilkårOppfylt,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis det ikke finnes noen vurderinger som trengs`() {
            val inngangsvilkår = OvergangssstønadTilsynBarn

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }
}
