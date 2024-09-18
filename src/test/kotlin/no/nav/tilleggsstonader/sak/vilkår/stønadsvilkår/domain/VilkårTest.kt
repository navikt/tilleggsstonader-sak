package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårStatus
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VilkårTest {

    private val behandlingIdFørstegangsbehandling = BehandlingId.random()
    private val behandlingIdRevurdering = BehandlingId.random()

    @Test
    internal fun `opprettOpphavsvilkår - et vilkår som ikke er gjenbrukt skal peke til behandlingen`() {
        val vilkår = Vilkår(
            behandlingId = behandlingIdFørstegangsbehandling,
            delvilkårwrapper = DelvilkårWrapper(emptyList()),
            type = VilkårType.EKSEMPEL,
            status = VilkårStatus.NY,
            opphavsvilkår = null,
        )
        val opphavsvilkår = vilkår.opprettOpphavsvilkår()
        Assertions.assertThat(opphavsvilkår).isEqualTo(
            Opphavsvilkår(
                behandlingIdFørstegangsbehandling,
                vilkår.sporbar.endret.endretTid,
            ),
        )
    }

    @Test
    internal fun `opprettOpphavsvilkår - skal bruke opphavsvilkår hvis den finnes og ikke lage en ny, for å peke til den opprinnelige behandlingen`() {
        val opphavsvilkår = Opphavsvilkår(behandlingIdFørstegangsbehandling, osloNow())
        val vilkår = Vilkår(
            behandlingId = behandlingIdRevurdering,
            delvilkårwrapper = DelvilkårWrapper(emptyList()),
            type = VilkårType.EKSEMPEL,
            opphavsvilkår = opphavsvilkår,
            status = VilkårStatus.UENDRET,
        )
        val nyttOpphavsvilkår = vilkår.opprettOpphavsvilkår()
        Assertions.assertThat(nyttOpphavsvilkår).isEqualTo(opphavsvilkår)
    }

    @Test
    fun `skal feile dersom FOM fra vilkår ikke er første dagen i måneden`() {
        assertThatThrownBy {
            vilkår(
                behandlingId = BehandlingId.random(),
                barnId = BarnId.random(),
                type = VilkårType.PASS_BARN,
                resultat = Vilkårsresultat.OPPFYLT,
                fom = LocalDate.of(2024, 8, 2),
                tom = LocalDate.of(2024, 9, 2),
                utgift = 1,
            )
        }.hasMessageContaining("For vilkår=PASS_BARN skal FOM være første dagen i måneden")
    }

    @Test
    fun `skal feile dersom TOM fra vilkår ikke er siste dagen i måneden`() {
        assertThatThrownBy {
            vilkår(
                behandlingId = BehandlingId.random(),
                barnId = BarnId.random(),
                type = VilkårType.PASS_BARN,
                resultat = Vilkårsresultat.OPPFYLT,
                fom = LocalDate.of(2024, 8, 1),
                tom = LocalDate.of(2024, 9, 2),
                utgift = 2,
            )
        }.hasMessageContaining("For vilkår=PASS_BARN skal TOM være siste dagen i måneden")
    }
}
