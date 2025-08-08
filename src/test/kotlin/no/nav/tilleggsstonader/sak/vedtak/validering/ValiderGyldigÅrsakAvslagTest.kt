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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ValiderGyldigÅrsakAvslagTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val validerGyldigÅrsakAvslag = ValiderGyldigÅrsakAvslag(vilkårperiodeService)

    val behandlingId = BehandlingId.random()

    @Nested
    inner class ValiderAktivitet {
        @ParameterizedTest
        @EnumSource(
            value = ÅrsakAvslag::class,
            names = [ "INGEN_AKTIVITET", "HAR_IKKE_UTGIFTER", "RETT_TIL_UTSTYRSSTIPEND"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `gyldig å avslå på aktivitet dersom det finnes en ikke oppfylt aktivitet`(årsakAvslag: ÅrsakAvslag) {
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

        @ParameterizedTest
        @EnumSource(
            value = ÅrsakAvslag::class,
            names = [ "INGEN_AKTIVITET", "HAR_IKKE_UTGIFTER", "RETT_TIL_UTSTYRSSTIPEND"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `gyldig å avslå på aktivitet dersom det finnes minst en ikke oppfylt aktivitet`(årsakAvslag: ÅrsakAvslag) {
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
                    listOf(årsakAvslag),
                )
            }
        }

        @ParameterizedTest
        @EnumSource(
            value = ÅrsakAvslag::class,
            names = [ "INGEN_AKTIVITET", "HAR_IKKE_UTGIFTER", "RETT_TIL_UTSTYRSSTIPEND"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal kaste feil om avslaggrunn er aktivitet dersom det ikke finnes en aktivitet med resultat ikke oppfylt`(
            årsakAvslag: ÅrsakAvslag,
        ) {
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
                    listOf(årsakAvslag),
                )
            }.hasMessageContaining("Kan ikke avslå med følgende årsak(er)")
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

    @ParameterizedTest
    @EnumSource(
        value = ÅrsakAvslag::class,
        names = ["IKKE_I_MÅLGRUPPE", "INGEN_AKTIVITET", "HAR_IKKE_UTGIFTER", "RETT_TIL_UTSTYRSSTIPEND"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal ikke kaste valideringsfeil dersom årsak ikke gjelder aktivitet eller målgruppe`(årsakAvslag: ÅrsakAvslag) {
        assertDoesNotThrow {
            validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(
                behandlingId,
                listOf(årsakAvslag),
            )
        }
    }
}
