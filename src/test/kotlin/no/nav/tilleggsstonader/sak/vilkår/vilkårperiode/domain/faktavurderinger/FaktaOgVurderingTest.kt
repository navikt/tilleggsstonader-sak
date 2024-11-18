package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingTest {

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

        val vurderingIkkeVurdert = Vurdering(svar = null, resultat = ResultatDelvilkårperiode.IKKE_VURDERT)
        val vurderingIkkeOppfylt = Vurdering(svar = null, resultat = ResultatDelvilkårperiode.IKKE_OPPFYLT)
        val vurderingOppfylt = Vurdering(svar = null, resultat = ResultatDelvilkårperiode.OPPFYLT)

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og en er oppfylt`() {
            val inngangsvilkår = NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    medlemskap = vurderingIkkeVurdert,
                    dekketAvAnnetRegelverk = vurderingOppfylt,
                ),
            )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og en er ikke oppfylt`() {
            val inngangsvilkår = NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    medlemskap = vurderingIkkeOppfylt,
                    dekketAvAnnetRegelverk = vurderingIkkeVurdert,
                ),
            )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYKT hvis en vurdering er oppfylt og en er ikke oppfylt`() {
            val inngangsvilkår = NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    medlemskap = vurderingOppfylt,
                    dekketAvAnnetRegelverk = vurderingIkkeOppfylt,
                ),
            )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
            val inngangsvilkår = NedsattArbeidsevneTilsynBarn(
                vurderinger = VurderingNedsattArbeidsevne(
                    medlemskap = vurderingOppfylt,
                    dekketAvAnnetRegelverk = vurderingOppfylt,
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
