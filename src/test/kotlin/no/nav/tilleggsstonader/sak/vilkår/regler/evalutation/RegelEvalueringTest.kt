package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.EksempelRegel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegelEvalueringTest {

    @Test
    fun `utledVilkårResultat - er OPPFYLT når alle vilkår er OPPFYLT`() {
        assertThat(RegelEvaluering.utledVilkårResultat(mapOf(RegelId.HAR_ET_NAVN to Vilkårsresultat.OPPFYLT)))
            .isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er IKKE_OPPFYLT når det finnes en med IKKE_OPPFYLT`() {
        assertThat(
            RegelEvaluering.utledVilkårResultat(
                mapOf(
                    RegelId.HAR_ET_NAVN to Vilkårsresultat.OPPFYLT,
                    RegelId.HAR_ET_NAVN2 to Vilkårsresultat.IKKE_OPPFYLT,
                ),
            ),
        )
            .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er OPPFYLT når alle er AUTOMATISK_OPPFYLT`() {
        assertThat(
            RegelEvaluering.utledVilkårResultat(
                mapOf(
                    RegelId.HAR_ET_NAVN to Vilkårsresultat.AUTOMATISK_OPPFYLT,
                ),
            ),
        )
            .isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - utledResultat skal gi OPPFYLT når delvilkår er AUTOMATISK_OPPFYLT`() {
        val vurderingDto = VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "begrunnelse")
        val delvilkår = DelvilkårDto(Vilkårsresultat.AUTOMATISK_OPPFYLT, listOf(vurderingDto))

        val regelResultat = RegelEvaluering.utledResultat(EksempelRegel(), listOf(delvilkår))
        assertThat(regelResultat.vilkår).isEqualTo(Vilkårsresultat.OPPFYLT)

        assertThat(regelResultat.delvilkår.keys.size).isEqualTo(1)
        assertThat(regelResultat.delvilkår.keys.first()).isEqualTo(RegelId.HAR_ET_NAVN)
        assertThat(regelResultat.delvilkår.values.size).isEqualTo(1)
        assertThat(regelResultat.delvilkår.values.first()).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledVilkårResultat - er IKKE_TATT_STILLING_TIL når det finnes en med IKKE_TATT_STILLING_TIL`() {
        assertThat(
            RegelEvaluering.utledVilkårResultat(
                mapOf(
                    RegelId.HAR_ET_NAVN to Vilkårsresultat.OPPFYLT,
                    RegelId.HAR_ET_NAVN2 to Vilkårsresultat.IKKE_OPPFYLT,
                    RegelId.HAR_ET_NAVN3 to Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                ),
            ),
        )
            .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}
