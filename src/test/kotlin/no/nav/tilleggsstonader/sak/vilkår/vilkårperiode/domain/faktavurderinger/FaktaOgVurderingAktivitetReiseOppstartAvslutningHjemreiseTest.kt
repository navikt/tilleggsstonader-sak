package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingLønnet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingAktivitetReiseOppstartAvslutningHjemreiseTest {
    @Test
    fun `resultatet skal ikke være oppfylt hvis ikke typen aktivitet gir rett på stønaden`() {
        listOf(
            IngenAktivitetReiseOppstartAvslutningHjemreiseTso,
            IngenAktivitetReiseOppstartAvslutningHjemreiseTsr,
        ).forEach { faktaOgVurdering ->
            assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }
    }

    @Test
    fun `utdanning tso og tiltak tsr skal alltid være oppfylt, da det ikke finnes noen vurderinger`() {
        assertThat(UtdanningReiseOppstartAvslutningHjemreiseTso.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        assertThat(TiltakReiseOppstartAvslutningHjemreiseTsr.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
    }

    object LønnetSvar {
        val ikkeVurdert = vurderingLønnet(svar = null)
        val ja = vurderingLønnet(svar = SvarJaNei.JA)
        val nei = vurderingLønnet(svar = SvarJaNei.NEI)
    }

    @Nested
    inner class TiltakTso {
        @Test
        fun `resultat er IKKE_VURDERT hvis lønnet ikke er vurdert`() {
            val inngangsvilkår =
                TiltakReiseOppstartAvslutningHjemreiseTso(
                    vurderinger = VurderingTiltakReiseOppstartAvslutningHjemreiseTso(lønnet = LønnetSvar.ikkeVurdert),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
        }

        @Test
        fun `resultat er IKKE_OPPFYLT hvis bruker mottar ordinær lønn`() {
            val inngangsvilkår =
                TiltakReiseOppstartAvslutningHjemreiseTso(
                    vurderinger = VurderingTiltakReiseOppstartAvslutningHjemreiseTso(lønnet = LønnetSvar.ja),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `resultat er OPPFYLT hvis bruker ikke mottar ordinær lønn`() {
            val inngangsvilkår =
                TiltakReiseOppstartAvslutningHjemreiseTso(
                    vurderinger = VurderingTiltakReiseOppstartAvslutningHjemreiseTso(lønnet = LønnetSvar.nei),
                )

            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }
    }
}
