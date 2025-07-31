package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.validering.ValiderGyldigÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BoutgifterValiderGyldigÅrsakAvslagTest {
    private val vilkårService = mockk<VilkårService>(relaxed = true)
    private val validerGyldigÅrsakAvslag = mockk<ValiderGyldigÅrsakAvslag>()
    private val boutgifterValiderGyldigÅrsakAvslag =
        BoutgifterValiderGyldigÅrsakAvslag(vilkårService, validerGyldigÅrsakAvslag)

    val behandlingId = BehandlingId.random()

    val oppfyltVilkår =
        vilkår(
            type = VilkårType.UTGIFTER_OVERNATTING,
            resultat = Vilkårsresultat.OPPFYLT,
            behandlingId = behandlingId,
        )

    val ikkeOppfyltVilkår =
        vilkår(
            type = VilkårType.UTGIFTER_OVERNATTING,
            resultat = Vilkårsresultat.IKKE_OPPFYLT,
            behandlingId = behandlingId,
        )

    @BeforeEach
    fun setup() {
        justRun { validerGyldigÅrsakAvslag.validerGyldigAvslagInngangsvilkår(any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(
        value = ÅrsakAvslag::class,
        names = ["MANGELFULL_DOKUMENTASJON", "HAR_IKKE_MERUTGIFTER", "RETT_TIL_BOSTØTTE"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `skal kaste feil ved stønadsvilkårårsak men kun oppfylte vilkår`(årsakAvslag: ÅrsakAvslag) {
        every { vilkårService.hentVilkår(behandlingId) } returns listOf(oppfyltVilkår)

        val vedtak =
            AvslagBoutgifterDto(
                årsakerAvslag = listOf(årsakAvslag),
                begrunnelse = "Begrunnelse",
            )

        assertThatThrownBy {
            boutgifterValiderGyldigÅrsakAvslag.validerGyldigAvslag(behandlingId, vedtak)
        }.hasMessageContaining("Kan ikke avslå med årsak '${årsakAvslag.displayName}'")
    }

    @ParameterizedTest
    @EnumSource(
        value = ÅrsakAvslag::class,
        names = ["MANGELFULL_DOKUMENTASJON", "HAR_IKKE_MERUTGIFTER", "RETT_TIL_BOSTØTTE"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `skal kaste feil ved stønadsvilkårårsak men ingen registrerte vilkår`(årsakAvslag: ÅrsakAvslag) {
        every { vilkårService.hentVilkår(behandlingId) } returns emptyList()

        val vedtak =
            AvslagBoutgifterDto(
                årsakerAvslag = listOf(årsakAvslag),
                begrunnelse = "Begrunnelse",
            )

        assertThatThrownBy {
            boutgifterValiderGyldigÅrsakAvslag.validerGyldigAvslag(behandlingId, vedtak)
        }.hasMessageContaining("Kan ikke avslå med årsak '${årsakAvslag.displayName}'")
    }

    @Test
    fun `skal kaste feil ved flere stønadsvilkårårsak men kun oppfylte vilkår`() {
        every { vilkårService.hentVilkår(behandlingId) } returns listOf(oppfyltVilkår)

        val vedtak =
            AvslagBoutgifterDto(
                årsakerAvslag = listOf(ÅrsakAvslag.HAR_IKKE_MERUTGIFTER, ÅrsakAvslag.MANGELFULL_DOKUMENTASJON),
                begrunnelse = "Begrunnelse",
            )

        assertThatThrownBy {
            boutgifterValiderGyldigÅrsakAvslag.validerGyldigAvslag(behandlingId, vedtak)
        }.hasMessageContaining(
            "Kan ikke avslå med årsak '${ÅrsakAvslag.HAR_IKKE_MERUTGIFTER.displayName}' og '${ÅrsakAvslag.MANGELFULL_DOKUMENTASJON.displayName}'",
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = ÅrsakAvslag::class,
        names = ["MANGELFULL_DOKUMENTASJON", "HAR_IKKE_MERUTGIFTER", "RETT_TIL_BOSTØTTE"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `tillater stønadsvilkårårsak med minst et ikke oppfylt vilkår`(årsakAvslag: ÅrsakAvslag) {
        every { vilkårService.hentVilkår(behandlingId) } returns listOf(oppfyltVilkår, ikkeOppfyltVilkår)

        val vedtak =
            AvslagBoutgifterDto(
                årsakerAvslag = listOf(årsakAvslag),
                begrunnelse = "Begrunnelse",
            )

        assertDoesNotThrow {
            boutgifterValiderGyldigÅrsakAvslag.validerGyldigAvslag(behandlingId, vedtak)
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = ÅrsakAvslag::class,
        names = ["MANGELFULL_DOKUMENTASJON", "HAR_IKKE_MERUTGIFTER", "RETT_TIL_BOSTØTTE"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal ikke kaste feil dersom årsak ikke er knyttet til stønadsvilkår`(årsakAvslag: ÅrsakAvslag) {
        val vedtak =
            AvslagBoutgifterDto(
                årsakerAvslag = listOf(årsakAvslag),
                begrunnelse = "Begrunnelse",
            )

        assertDoesNotThrow {
            boutgifterValiderGyldigÅrsakAvslag.validerGyldigAvslag(behandlingId, vedtak)
        }
    }
}