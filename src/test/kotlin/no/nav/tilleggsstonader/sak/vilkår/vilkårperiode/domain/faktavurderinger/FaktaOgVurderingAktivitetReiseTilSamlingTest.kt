package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingAktivitetReiseTilSamlingTest {
    @Test
    fun `resultatet skal ikke være oppfylt hvis ikke typen aktivitet gir rett på stønaden`() {
        listOf(IngenAktivitetReiseTilSamlingTso, IngenAktivitetReiseTilSamlingTsr).forEach { faktaOgVurdering ->
            assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }
    }

    @Test
    fun `utdanning tso skal alltid være oppfylt, da det ikke finnes noen vurderinger`() {
        assertThat(UtdanningReiseTilSamlingTso.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

    @Test
    fun `tiltak og utdanning tsr skal alltid være oppfylt, da det ikke finnes noen vurderinger`() {
        assertThat(TiltakReiseTilSamlingTsr.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        assertThat(UtdanningReiseTilSamlingTsr.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

    object LønnetSvar {
        val ikkeVurdert = vurderingLønnet(svar = null)
        val ja = vurderingLønnet(svar = SvarJaNei.JA)
        val nei = vurderingLønnet(svar = SvarJaNei.NEI)
    }

    @Nested
    inner class Tiltak {
        @Test
        fun `resultat er IKKE_VURDERT hvis lønnet ikke er vurdert`() {
            val inngangsvilkår =
                TiltakReiseTilSamlingTso(
                    vurderinger = VurderingTiltakReiseTilSamlingTso(lønnet = LønnetSvar.ikkeVurdert),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYLT hvis bruker mottar ordinær lønn`() {
            val inngangsvilkår =
                TiltakReiseTilSamlingTso(
                    vurderinger = VurderingTiltakReiseTilSamlingTso(lønnet = LønnetSvar.ja),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis bruker ikke mottar ordinær lønn`() {
            val inngangsvilkår =
                TiltakReiseTilSamlingTso(
                    vurderinger = VurderingTiltakReiseTilSamlingTso(lønnet = LønnetSvar.nei),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }
}
