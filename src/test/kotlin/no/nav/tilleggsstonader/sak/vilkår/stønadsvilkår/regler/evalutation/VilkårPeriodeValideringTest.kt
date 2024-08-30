package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.VilkårPeriodeValidering.validerIkkeOverlappendeVilkår
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate.now
import java.util.UUID

class VilkårPeriodeValideringTest {

    val behandlingId = UUID.randomUUID()
    val barnId = UUID.randomUUID()

    @Test
    fun `skal ikke kaste feil hvis 2 perioder uten barnId ikke overlapper`() {
        val vilkår = vilkår(behandlingId = behandlingId, fom = now(), tom = now(), beløp = 1)
        val vilkår2 = vilkår.copy(fom = now().plusDays(1), tom = now().plusDays(1))

        validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår2))
    }

    @Test
    fun `skal kaste feil hvis 2 perioder uten barnId overlapper`() {
        val vilkår = vilkår(behandlingId = behandlingId, fom = now(), tom = now(), beløp = 1)

        assertThatThrownBy {
            validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår))
        }.hasMessageContaining(" overlapper")
    }

    @Test
    fun `skal ikke kaste feil hvis 2 perioder for samme barn ikke overlapper`() {
        val vilkår = vilkår(behandlingId = behandlingId, barnId = barnId, fom = now(), tom = now(), beløp = 1)
        val vilkår2 = vilkår.copy(fom = now().plusDays(1), tom = now().plusDays(1))

        validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår2))
    }

    @Test
    fun `skal kaste feil hvis 2 perioder for samme barn overlapper`() {
        val vilkår = vilkår(behandlingId = behandlingId, barnId = barnId, fom = now(), tom = now(), beløp = 1)
        assertThatThrownBy {
            validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår))
        }.hasMessageContaining(" overlapper")
    }

    @Test
    fun `skal ikke kaste feil hvis 2 perioder for ulike barn ikke overlapper`() {
        val vilkår = vilkår(behandlingId = behandlingId, barnId = barnId, fom = now(), tom = now(), beløp = 1)
        val vilkår2 = vilkår.copy(barnId = UUID.randomUUID())

        validerIkkeOverlappendeVilkår(listOf(vilkår, vilkår2))
    }
}
