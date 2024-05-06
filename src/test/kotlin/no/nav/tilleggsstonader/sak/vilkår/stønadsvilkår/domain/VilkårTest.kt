package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.libs.utils.osloNow
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VilkårTest {

    private val behandlingIdFørstegangsbehandling = UUID.randomUUID()
    private val behandlingIdRevurdering = UUID.randomUUID()

    @Test
    internal fun `opprettOpphavsvilkår - et vilkår som ikke er gjenbrukt skal peke til behandlingen`() {
        val vilkår = Vilkår(
            behandlingId = behandlingIdFørstegangsbehandling,
            delvilkårwrapper = DelvilkårWrapper(emptyList()),
            type = VilkårType.EKSEMPEL,
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
        )
        val nyttOpphavsvilkår = vilkår.opprettOpphavsvilkår()
        Assertions.assertThat(nyttOpphavsvilkår).isEqualTo(opphavsvilkår)
    }
}
