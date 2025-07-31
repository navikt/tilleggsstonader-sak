package no.nav.tilleggsstonader.sak.vedtak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ValiderGyldigÅrsakAvslagTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val validerGyldigÅrsakAvslag = ValiderGyldigÅrsakAvslag(vilkårperiodeService)

    val behandlingId = BehandlingId.random()

    @Nested
    inner class ValiderAktivitet {
        @Test
        fun `INGEN AKTIVITET er gyldig avslagårsak dersom det finnes en ikke oppfylt aktivitet`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                    Vilkårperioder(
                        aktiviteter =
                            listOf(
                                aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                            ),
                        målgrupper = emptyList(),
                    )

            assertDoesNotThrow {
                validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                    behandlingId,
                    listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                )
            }
        }

        @Test
        fun `INGEN AKTIVITET er gyldig avslagårsak dersom det finnes minst en ikke oppfylt aktivitet`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                    Vilkårperioder(
                        aktiviteter =
                            listOf(
                                aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                                aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                            ),
                        målgrupper = emptyList(),
                    )

            assertDoesNotThrow {
                validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                    behandlingId,
                    listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                )
            }
        }

        @Test
        fun `INGEN AKTIVITET er ikke gyldig avslagsgrunn dersom det ikke finnes en aktivitet med resultat ikke oppfylt`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                    Vilkårperioder(
                        aktiviteter =
                            listOf(
                                aktivitet(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                            ),
                        målgrupper = emptyList(),
                    )

            assertThatThrownBy {
                validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                    behandlingId,
                    listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                )
            }.hasMessageContaining("Kan ikke avslå med årsak '${ÅrsakAvslag.INGEN_AKTIVITET.displayName}'")
        }
    }

    @Nested
    inner class ValiderMålgruppe {
        @Test
        fun `IKKE I MÅLGRUPPE er gyldig avslagårsak dersom det finnes en ikke oppfylt målgruppe`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                    Vilkårperioder(
                        aktiviteter = emptyList(),
                        målgrupper =
                            listOf(
                                målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                            ),
                    )

            assertDoesNotThrow {
                validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                    behandlingId,
                    listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                )
            }
        }

        @Test
        fun `IKKE I MÅLGRUPPE er gyldig avslagårsak dersom det finnes minst en ikke oppfylt målgruppe`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                    Vilkårperioder(
                        aktiviteter = emptyList(),
                        målgrupper =
                            listOf(
                                målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                                målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                            ),
                    )

            assertDoesNotThrow {
                validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                    behandlingId,
                    listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                )
            }
        }

        @Test
        fun `IKKE I MÅLGRUPPE er ikke gyldig hvis det ikke er lagt inn en målgruppe som ikke er oppfylt`() {
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
                    Vilkårperioder(
                        aktiviteter = emptyList(),
                        målgrupper =
                            listOf(
                                målgruppe(behandlingId = behandlingId, resultat = ResultatVilkårperiode.OPPFYLT),
                            ),
                    )

            assertThatThrownBy {
                validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                    behandlingId,
                    listOf(ÅrsakAvslag.IKKE_I_MÅLGRUPPE),
                )
            }.hasMessageContaining("Kan ikke avslå med årsak '${ÅrsakAvslag.IKKE_I_MÅLGRUPPE.displayName}'")
        }
    }

    @Test
    fun `skal ikke kaste valideringsfeil dersom årsak ikke gjelder aktivitet elelr målgruppe`() {
        assertDoesNotThrow {
            validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                behandlingId,
                listOf(ÅrsakAvslag.HAR_IKKE_UTGIFTER),
            )
        }
    }
}
