package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.validerVilkårOgBeregnResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class OppdaterVilkårTest {

    val behandlingId = UUID.randomUUID()
    val vilkår = vilkår(
        behandlingId = behandlingId,
        type = VilkårType.PASS_BARN,
        delvilkår = oppfylteDelvilkårPassBarn(),
    )

    @Nested
    inner class ValideringAvBeløp {
        val opprettVilkårDto = OpprettVilkårDto(
            vilkårType = VilkårType.PASS_BARN,
            barnId = UUID.randomUUID(),
            behandlingId = behandlingId,
            delvilkårsett = oppfylteDelvilkårPassBarnDto(),
            fom = LocalDate.now(),
            tom = LocalDate.now().plusDays(1),
            beløp = 1,
        )

        @Test
        fun `skal validere at man har med beløp for vilkår for pass av barn`() {
            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(fom = null), true)
            }.hasMessageContaining("Mangler fom/tom på vilkår")

            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(tom = null), true)
            }.hasMessageContaining("Mangler fom/tom på vilkår")
        }

        @Test
        fun `skal validere at pass av barn inneholkder beløp`() {
            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(beløp = null), true)
            }.hasMessageContaining("Mangler beløp på vilkår")
        }

        @Disabled // TODO fiks når annen type enn eksempel er tilgjengelig
        @Test
        fun `skal validere at annet vilkår enn pass av barn ikke inneholder beløp`() {
            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(vilkårType = VilkårType.EKSEMPEL), true)
            }.hasMessageContaining("Mangler beløp på vilkår")
        }
    }
}
