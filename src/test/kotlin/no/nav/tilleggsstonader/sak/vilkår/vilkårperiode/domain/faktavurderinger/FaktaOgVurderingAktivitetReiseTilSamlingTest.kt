package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingHarUtgifter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingAktivitetReiseTilSamlingTest {
    @Test
    fun `resultatet skal ikke være oppfylt hvis ikke typen aktivitet gir rett på stønaden`() {
        listOf(IngenAktivitetReiseTilSamlingTso).forEach { faktaOgVurdering ->
            assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }
    }

    object HarUtgifterSvar {
        val ikkeVurdert = vurderingHarUtgifter(svar = null)
        val ja = vurderingHarUtgifter(svar = SvarJaNei.JA)
        val nei = vurderingHarUtgifter(svar = SvarJaNei.NEI)
    }

    object ErAktivitetenObligatoriskSvar {
        val ikkeVurdert = VurderingErAktivitetenObligatorisk(svar = null)
        val ja = VurderingErAktivitetenObligatorisk(svar = SvarJaNei.JA)
        val nei = VurderingErAktivitetenObligatorisk(svar = SvarJaNei.NEI)
    }

    object LønnetSvar {
        val ikkeVurdert = vurderingLønnet(svar = null)
        val ja = vurderingLønnet(svar = SvarJaNei.JA)
        val nei = vurderingLønnet(svar = SvarJaNei.NEI)
    }

    @Nested
    inner class Utdanning {
        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og resten er oppfylt`() {
            val inngangsvilkår =
                UtdanningReiseTilSamlingTso(
                    vurderinger =
                        VurderingUtdanningReiseTilSamlingTso(
                            harUtgifter = HarUtgifterSvar.ja,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.ikkeVurdert,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og resten er ikke oppfylt`() {
            val inngangsvilkår =
                UtdanningReiseTilSamlingTso(
                    vurderinger =
                        VurderingUtdanningReiseTilSamlingTso(
                            harUtgifter = HarUtgifterSvar.nei,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.ikkeVurdert,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYLT hvis minst en vurdering er ikke oppfylt`() {
            val inngangsvilkår =
                UtdanningReiseTilSamlingTso(
                    vurderinger =
                        VurderingUtdanningReiseTilSamlingTso(
                            harUtgifter = HarUtgifterSvar.ja,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.nei,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
            val inngangsvilkår =
                UtdanningReiseTilSamlingTso(
                    vurderinger =
                        VurderingUtdanningReiseTilSamlingTso(
                            harUtgifter = HarUtgifterSvar.ja,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.ja,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }

    @Nested
    inner class Tiltak {
        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og resten er oppfylt`() {
            val inngangsvilkår =
                TiltakReiseTilSamlingTso(
                    vurderinger =
                        VurderingTiltakReiseTilSamlingTso(
                            lønnet = LønnetSvar.nei,
                            harUtgifter = HarUtgifterSvar.ja,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.ikkeVurdert,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_VURDERT hvis en vurdering ikke er vurdert og resten er ikke oppfylt`() {
            val inngangsvilkår =
                TiltakReiseTilSamlingTso(
                    vurderinger =
                        VurderingTiltakReiseTilSamlingTso(
                            lønnet = LønnetSvar.ja,
                            harUtgifter = HarUtgifterSvar.ja,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.ikkeVurdert,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYLT hvis minst en vurdering er ikke oppfylt`() {
            val inngangsvilkår =
                TiltakReiseTilSamlingTso(
                    vurderinger =
                        VurderingTiltakReiseTilSamlingTso(
                            lønnet = LønnetSvar.ja,
                            harUtgifter = HarUtgifterSvar.ja,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.ja,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis alle vurderinger har resultat oppfylt`() {
            val inngangsvilkår =
                TiltakReiseTilSamlingTso(
                    vurderinger =
                        VurderingTiltakReiseTilSamlingTso(
                            lønnet = LønnetSvar.nei,
                            harUtgifter = HarUtgifterSvar.ja,
                            erAktivitetenObligatorisk = ErAktivitetenObligatoriskSvar.ja,
                        ),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }
}
