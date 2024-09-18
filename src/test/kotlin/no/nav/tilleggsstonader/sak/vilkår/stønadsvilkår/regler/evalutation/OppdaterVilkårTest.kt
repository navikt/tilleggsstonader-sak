package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.oppdaterVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.validerVilkårOgBeregnResultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.ikkeOppfylteDelvilkårPassBarnDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdaterVilkårTest {

    val behandlingId = BehandlingId.random()
    val vilkår = vilkår(
        behandlingId = behandlingId,
        type = VilkårType.PASS_BARN,
        delvilkår = oppfylteDelvilkårPassBarn(),
    )

    @Nested
    inner class ValideringAvBeløp {
        val opprettVilkårDto = OpprettVilkårDto(
            vilkårType = VilkårType.PASS_BARN,
            barnId = BarnId.random(),
            behandlingId = behandlingId,
            delvilkårsett = oppfylteDelvilkårPassBarnDto(),
            fom = LocalDate.now(),
            tom = LocalDate.now().plusDays(1),
            utgift = 1,
        )

        @Test
        fun `skal validere at man har med beløp for vilkår for pass av barn`() {
            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(fom = null))
            }.hasMessageContaining("Mangler fra og med/til og med på vilkår")

            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(tom = null))
            }.hasMessageContaining("Mangler fra og med/til og med på vilkår")
        }

        @Test
        fun `skal kaste feil hvis innvilget pass av barn ikke inneholder beløp`() {
            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(utgift = null))
            }.hasMessageContaining("Mangler utgift på vilkår")
        }

        @Test
        fun `skal ikke kaste feil hvis ikke oppfylt pass av barn ikke inneholder beløp`() {
            val dto = opprettVilkårDto.copy(utgift = null, delvilkårsett = ikkeOppfylteDelvilkårPassBarnDto())
            validerVilkårOgBeregnResultat(vilkår, dto)
        }

        @Disabled // TODO fiks når annen type enn eksempel er tilgjengelig
        @Test
        fun `skal validere at annet vilkår enn pass av barn ikke inneholder beløp`() {
            assertThatThrownBy {
                validerVilkårOgBeregnResultat(vilkår, opprettVilkårDto.copy(vilkårType = VilkårType.EKSEMPEL))
            }.hasMessageContaining("Mangler beløp på vilkår")
        }
    }

    @Nested
    inner class StatusPåVilkår {
        val behandling = behandling()
        val regelResultat = RegelResultat(
            vilkårType = VilkårType.PASS_BARN,
            vilkår = Vilkårsresultat.OPPFYLT,
            delvilkår = emptyMap(),
        )

        @Test
        fun `Nye vilkår skal ha status ny etter oppdatering`() {
            val vilkår = vilkår(behandling.id, type = VilkårType.PASS_BARN, status = VilkårStatus.NY)
            val innsendtOppdatering = innsendtOppdatering(vilkår)

            val oppdaterVilkår =
                oppdaterVilkår(vilkår = vilkår, oppdatering = innsendtOppdatering, vilkårsresultat = regelResultat)

            assertThat(oppdaterVilkår.status).isEqualTo(VilkårStatus.NY)
        }

        @Test
        fun `Vilkår med status endret skal ikke endre status ved oppdatering`() {
            val vilkår = vilkår(behandling.id, type = VilkårType.PASS_BARN, status = VilkårStatus.ENDRET)
            val innsendtOppdatering = innsendtOppdatering(vilkår)

            val oppdaterVilkår =
                oppdaterVilkår(vilkår = vilkår, oppdatering = innsendtOppdatering, vilkårsresultat = regelResultat)

            assertThat(oppdaterVilkår.status).isEqualTo(VilkårStatus.ENDRET)
        }

        @Test
        fun `Vilkår med status uendret skal få status endret etter oppdatering`() {
            val vilkår = vilkår(behandling.id, type = VilkårType.PASS_BARN, status = VilkårStatus.UENDRET)
            val innsendtOppdatering = innsendtOppdatering(vilkår)

            val oppdaterVilkår =
                oppdaterVilkår(vilkår = vilkår, oppdatering = innsendtOppdatering, vilkårsresultat = regelResultat)

            assertThat(oppdaterVilkår.status).isEqualTo(VilkårStatus.ENDRET)
        }

        private fun innsendtOppdatering(originaltVilkår: Vilkår) = SvarPåVilkårDto(
            id = originaltVilkår.id,
            behandlingId = behandling.id,
            delvilkårsett = oppfylteDelvilkårPassBarnDto(),
            fom = originaltVilkår.fom,
            tom = originaltVilkår.tom,
            utgift = 100,
        )
    }
}
