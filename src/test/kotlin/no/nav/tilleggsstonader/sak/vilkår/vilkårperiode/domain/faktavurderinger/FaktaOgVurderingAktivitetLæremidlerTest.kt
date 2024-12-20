package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FaktaOgVurderingAktivitetLæremidlerTest {

    @Test
    fun `resultatet skal ikke være oppfylt hvis ikke aktivitetstypen gir rett på stønaden`() {
        listOf(IngenAktivitetLæremidler).forEach { faktaOgVurdering ->
            assertThat(faktaOgVurdering.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }
    }

    object HarUtgifterSvar {
        val ikkeVurdert = VurderingHarUtgifter(svar = null)
        val ja = VurderingHarUtgifter(svar = SvarJaNei.JA)
        val nei = VurderingHarUtgifter(svar = SvarJaNei.NEI)
    }

    object HarRettTilUtstyrsstipendSvar {
        val ikkeVurdert = VurderingHarRettTilUtstyrsstipend(svar = null)
        val ja = VurderingHarRettTilUtstyrsstipend(svar = SvarJaNei.JA)
        val nei = VurderingHarRettTilUtstyrsstipend(svar = SvarJaNei.NEI)
        val alleSvar = listOf(ikkeVurdert, ja, nei)
    }

    object StudienivåSvar {
        val ikkeSvart = FaktaAktivitetLæremidler(prosent = 100, studienivå = null)
        val høyereUtdanning =
            FaktaAktivitetLæremidler(prosent = 100, studienivå = Studienivå.HØYERE_UTDANNING)
        val videregående =
            FaktaAktivitetLæremidler(prosent = 100, studienivå = Studienivå.VIDEREGÅENDE)
        val allePermutasjoner = listOf(ikkeSvart, høyereUtdanning, videregående)
    }

    @Nested
    inner class Utdanning {
        @Test
        fun `hvis bruker ikke har utgifter skal resultatet alltid bli IKKE_OPPFYLT`() {
            for (studienivå in StudienivåSvar.allePermutasjoner) {
                for (harRettTilUtstyrsstipendSvar in HarRettTilUtstyrsstipendSvar.alleSvar) {
                    val inngangsvilkår = UtdanningLæremidler(
                        fakta = studienivå,
                        vurderinger = VurderingerUtdanningLæremidler(
                            harUtgifter = HarUtgifterSvar.nei,
                            harRettTilUtstyrsstipend = harRettTilUtstyrsstipendSvar,
                        ),
                    )
                    assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                }
            }
        }

        @Test
        fun `hvis bruker har utgifter tar høyere utdanning, så skal resultatet bli OPPFYLT`() {
            for (harRettTilUtstyrsstipendSvar in HarRettTilUtstyrsstipendSvar.alleSvar) {
                val inngangsvilkår = UtdanningLæremidler(
                    fakta = StudienivåSvar.høyereUtdanning,
                    vurderinger = VurderingerUtdanningLæremidler(
                        harUtgifter = HarUtgifterSvar.ja,
                        harRettTilUtstyrsstipend = harRettTilUtstyrsstipendSvar,
                    ),
                )
                assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            }
        }

        @Test
        fun `hvis bruker har utgifter og går på videregående skole uten rett til utstrysstipend, så skal resultatet bli OPPFYLT`() {
            val inngangsvilkår = UtdanningLæremidler(
                fakta = StudienivåSvar.videregående,
                vurderinger = VurderingerUtdanningLæremidler(
                    harUtgifter = HarUtgifterSvar.ja,
                    harRettTilUtstyrsstipend = HarRettTilUtstyrsstipendSvar.nei,
                ),
            )
            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @Test
        fun `hvis bruker har utgifter og går på videregående, men har rett til utstrysstipend, så skal resultatet bli IKKE_OPPFYLT`() {
            val inngangsvilkår = UtdanningLæremidler(
                fakta = StudienivåSvar.videregående,
                vurderinger = VurderingerUtdanningLæremidler(
                    harUtgifter = HarUtgifterSvar.ja,
                    harRettTilUtstyrsstipend = HarRettTilUtstyrsstipendSvar.ja,
                ),
            )
            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `hvis bruker ikke har vurdert utgifter skal resultatet alltid bli IKKE_VURDERT`() {
            for (studienivå in StudienivåSvar.allePermutasjoner) {
                for (harRettTilUtstyrsstipendSvar in HarRettTilUtstyrsstipendSvar.alleSvar) {
                    val inngangsvilkår = UtdanningLæremidler(
                        fakta = studienivå,
                        vurderinger = VurderingerUtdanningLæremidler(
                            harUtgifter = HarUtgifterSvar.ikkeVurdert,
                            harRettTilUtstyrsstipend = harRettTilUtstyrsstipendSvar,
                        ),
                    )
                    assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
                }
            }
        }
    }

    @Nested
    inner class Tiltak {
        @Test
        fun `hvis bruker ikke har utgifter skal resultatet alltid bli IKKE_OPPFYLT`() {
            for (studienivå in StudienivåSvar.allePermutasjoner) {
                for (harRettTilUtstyrsstipendSvar in HarRettTilUtstyrsstipendSvar.alleSvar) {
                    val inngangsvilkår = TiltakLæremidler(
                        fakta = studienivå,
                        vurderinger = VurderingTiltakLæremidler(
                            harUtgifter = HarUtgifterSvar.nei,
                            harRettTilUtstyrsstipend = harRettTilUtstyrsstipendSvar,
                        ),
                    )
                    assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
                }
            }
        }

        @Test
        fun `hvis bruker har utgifter tar høyere utdanning, så skal resultatet bli OPPFYLT`() {
            for (harRettTilUtstyrsstipendSvar in HarRettTilUtstyrsstipendSvar.alleSvar) {
                val inngangsvilkår = TiltakLæremidler(
                    fakta = StudienivåSvar.høyereUtdanning,
                    vurderinger = VurderingTiltakLæremidler(
                        harUtgifter = HarUtgifterSvar.ja,
                        harRettTilUtstyrsstipend = harRettTilUtstyrsstipendSvar,
                    ),
                )
                assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            }
        }

        @Test
        fun `hvis bruker har utgifter og går på videregående skole uten rett til utstrysstipend, så skal resultatet bli OPPFYLT`() {
            val inngangsvilkår = TiltakLæremidler(
                fakta = StudienivåSvar.videregående,
                vurderinger = VurderingTiltakLæremidler(
                    harUtgifter = HarUtgifterSvar.ja,
                    harRettTilUtstyrsstipend = HarRettTilUtstyrsstipendSvar.nei,
                ),
            )
            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.OPPFYLT)
        }

        @Test
        fun `hvis bruker har utgifter og går på videregående, men har rett til utstrysstipend, så skal resultatet bli IKKE_OPPFYLT`() {
            val inngangsvilkår = TiltakLæremidler(
                fakta = StudienivåSvar.videregående,
                vurderinger = VurderingTiltakLæremidler(
                    harUtgifter = HarUtgifterSvar.ja,
                    harRettTilUtstyrsstipend = HarRettTilUtstyrsstipendSvar.ja,
                ),
            )
            assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
        }

        @Test
        fun `hvis bruker ikke har vurdert utgifter skal resultatet alltid bli IKKE_VURDERT`() {
            for (studienivå in StudienivåSvar.allePermutasjoner) {
                for (harRettTilUtstyrsstipendSvar in HarRettTilUtstyrsstipendSvar.alleSvar) {
                    val inngangsvilkår = TiltakLæremidler(
                        fakta = studienivå,
                        vurderinger = VurderingTiltakLæremidler(
                            harUtgifter = HarUtgifterSvar.ikkeVurdert,
                            harRettTilUtstyrsstipend = harRettTilUtstyrsstipendSvar,
                        ),
                    )
                    assertThat(inngangsvilkår.utledResultat()).isEqualTo(ResultatVilkårperiode.IKKE_VURDERT)
                }
            }
        }
    }
}
