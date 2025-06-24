package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vilkår
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class VilkårTest {
    private val behandlingIdFørstegangsbehandling = BehandlingId.random()
    private val behandlingIdRevurdering = BehandlingId.random()

    @Test
    internal fun `opprettOpphavsvilkår - et vilkår som ikke er gjenbrukt skal peke til behandlingen`() {
        val vilkår =
            vilkår(
                behandlingId = behandlingIdFørstegangsbehandling,
                delvilkår = emptyList(),
                type = VilkårType.EKSEMPEL,
                status = VilkårStatus.NY,
                opphavsvilkår = null,
            )
        val nyttOpphavsvilkår = vilkår.kopierTilBehandling(BehandlingId.random(), BarnId.random()).opphavsvilkår!!
        assertThat(nyttOpphavsvilkår).isEqualTo(
            Opphavsvilkår(
                behandlingIdFørstegangsbehandling,
                vilkår.sporbar.endret.endretTid,
            ),
        )
    }

    /*
     * opprettOpphavsvilkår - skal bruke opphavsvilkår hvis den finnes og ikke lage en ny, for å peke til den
     * opprinnelige behandlingen`
     */
    @Test
    internal fun `opprettOpphavsvilkår skal bruke opphavsvilkår hvis den finnes`() {
        val opphavsvilkår = Opphavsvilkår(behandlingIdFørstegangsbehandling, LocalDateTime.now())
        val vilkår =
            vilkår(
                behandlingId = behandlingIdRevurdering,
                delvilkår = emptyList(),
                type = VilkårType.EKSEMPEL,
                opphavsvilkår = opphavsvilkår,
                status = VilkårStatus.UENDRET,
            )
        val nyttOpphavsvilkår = vilkår.kopierTilBehandling(BehandlingId.random(), BarnId.random()).opphavsvilkår
        assertThat(nyttOpphavsvilkår).isEqualTo(opphavsvilkår)
    }

    @Test
    internal fun `opprettOpphavsvilkår - skal ikke bruke opphavsvilkår hvis den finnes og vilkåret er endret`() {
        val opphavsvilkår = Opphavsvilkår(behandlingIdFørstegangsbehandling, LocalDateTime.now())
        val vilkår =
            vilkår(
                behandlingId = behandlingIdRevurdering,
                delvilkår = emptyList(),
                type = VilkårType.EKSEMPEL,
                opphavsvilkår = opphavsvilkår,
                status = VilkårStatus.ENDRET,
            )
        val nyttOpphavsvilkår = vilkår.kopierTilBehandling(BehandlingId.random(), BarnId.random()).opphavsvilkår!!
        assertThat(nyttOpphavsvilkår.vurderingstidspunkt).isEqualTo(vilkår.sporbar.endret.endretTid)
        assertThat(nyttOpphavsvilkår.behandlingId).isEqualTo(vilkår.behandlingId)
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
