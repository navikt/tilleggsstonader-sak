package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.VilkårPeriodeValidering.validerIkkeOverlappendeVilkår
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårPeriodeValideringTest {

    val behandlingId = BehandlingId.random()
    val barnId = BarnId.random()

    val fom = LocalDate.of(2024, 1, 1)
    val tom = LocalDate.of(2024, 1, 31)

    @Test
    fun `skal ikke kaste feil hvis 2 perioder uten barnId ikke overlapper`() {
        val vilkår = vilkår(behandlingId = behandlingId, type = VilkårType.PASS_BARN, fom = fom, tom = tom, utgift = 1)
        val vilkår2 = vilkår.copy(fom = fom.plusMonths(1), tom = tom.plusMonths(1))

        validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår2))
    }

    @Test
    fun `skal kaste feil hvis 2 perioder uten barnId overlapper`() {
        val vilkår = vilkår(behandlingId = behandlingId, type = VilkårType.PASS_BARN, fom = fom, tom = tom, utgift = 1)

        assertThatThrownBy {
            validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår))
        }.hasMessageContaining(" overlapper")
    }

    @Test
    fun `skal ikke kaste feil hvis 2 perioder for samme barn ikke overlapper`() {
        val vilkår = vilkår(
            behandlingId = behandlingId,
            type = VilkårType.PASS_BARN,
            barnId = barnId,
            fom = fom,
            tom = tom,
            utgift = 1,
        )
        val vilkår2 = vilkår.copy(fom = fom.plusMonths(1), tom = tom.plusMonths(1))

        validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår2))
    }

    @Test
    fun `skal kaste feil hvis 2 perioder for samme barn overlapper`() {
        val vilkår = vilkår(
            behandlingId = behandlingId,
            type = VilkårType.PASS_BARN,
            barnId = barnId,
            fom = fom,
            tom = tom,
            utgift = 1,
        )
        assertThatThrownBy {
            validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår))
        }.hasMessageContaining(" overlapper")
    }

    @Test
    fun `skal ikke kaste feil hvis 2 perioder for ulike barn ikke overlapper`() {
        val vilkår = vilkår(
            behandlingId = behandlingId,
            type = VilkårType.PASS_BARN,
            barnId = barnId,
            fom = fom,
            tom = tom,
            utgift = 1,
        )
        val vilkår2 = vilkår.copy(barnId = BarnId.random())

        validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår2))
    }
}
