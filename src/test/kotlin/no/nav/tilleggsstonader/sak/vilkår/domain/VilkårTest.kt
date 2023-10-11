package no.nav.tilleggsstonader.sak.vilkår.domain

import org.assertj.core.api.Assertions.assertThat
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
        assertThat(opphavsvilkår).isEqualTo(
            Opphavsvilkår(
                behandlingIdFørstegangsbehandling,
                vilkår.sporbar.endret.endretTid,
            ),
        )
    }

    @Test
    internal fun `opprettOpphavsvilkår - skal bruke opphavsvilkår hvis den finnes og ikke lage en ny, for å peke til den opprinnelige behandlingen`() {
        val opphavsvilkår = Opphavsvilkår(behandlingIdFørstegangsbehandling, LocalDateTime.now())
        val vilkår = Vilkår(
            behandlingId = behandlingIdRevurdering,
            delvilkårwrapper = DelvilkårWrapper(emptyList()),
            type = VilkårType.EKSEMPEL,
            opphavsvilkår = opphavsvilkår,
        )
        val nyttOpphavsvilkår = vilkår.opprettOpphavsvilkår()
        assertThat(nyttOpphavsvilkår).isEqualTo(opphavsvilkår)
    }
}
