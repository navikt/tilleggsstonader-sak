package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ParentVilkårResultatTest {
    private val behandlingId = BehandlingId.random()

    @Test
    fun `reise er oppfylt når paraplyen er oppfylt`() {
        val paraply = paraply(Vilkårsresultat.OPPFYLT)
        val reise = reise(paraply, Vilkårsresultat.OPPFYLT)

        val resultat = listOf(paraply, reise).medResultatFraParent()

        assertThat(resultat.single { it.id == reise.id }.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    // AUTOMATISK_OPPFYLT er forbeholdt delvilkår og kan ikke være resultat på et vilkår
    @ParameterizedTest
    @EnumSource(Vilkårsresultat::class, names = ["OPPFYLT", "AUTOMATISK_OPPFYLT"], mode = EnumSource.Mode.EXCLUDE)
    fun `reise er ikke oppfylt når paraplyen ikke er oppfylt, uansett paraplyens resultat`(paraplyResultat: Vilkårsresultat) {
        val paraply = paraply(paraplyResultat)
        val reise = reise(paraply, Vilkårsresultat.OPPFYLT)

        val resultat = listOf(paraply, reise).medResultatFraParent()

        assertThat(resultat.single { it.id == reise.id }.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `reise er ikke oppfylt når paraplyen mangler`() {
        val reise = reise(paraply(Vilkårsresultat.OPPFYLT), Vilkårsresultat.OPPFYLT)

        val resultat = listOf(reise).medResultatFraParent()

        assertThat(resultat.single().resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `reisens eget resultat og alle andre felt beholdes, og gjelder igjen når paraplyen blir oppfylt`() {
        val ikkeOppfyltParaply = paraply(Vilkårsresultat.IKKE_OPPFYLT)
        val reise = reise(ikkeOppfyltParaply, Vilkårsresultat.OPPFYLT)

        val utenParaplyOppfylt = listOf(ikkeOppfyltParaply, reise).medResultatFraParent().single { it.id == reise.id }
        val medParaplyOppfylt =
            listOf(ikkeOppfyltParaply.copy(resultat = Vilkårsresultat.OPPFYLT), reise).medResultatFraParent().single { it.id == reise.id }

        assertThat(utenParaplyOppfylt).isEqualTo(reise.copy(resultat = Vilkårsresultat.IKKE_OPPFYLT))
        assertThat(medParaplyOppfylt).isEqualTo(reise)
    }

    @Test
    fun `reise uten paraply og selve paraplyen påvirkes ikke`() {
        val paraply = paraply(Vilkårsresultat.IKKE_OPPFYLT)
        val vanligReise = vilkår(behandlingId = behandlingId, type = VilkårType.ROAH_OPPSTART_AVSLUTNING)

        val resultat = listOf(paraply, vanligReise).medResultatFraParent()

        assertThat(resultat).containsExactly(paraply, vanligReise)
    }

    private fun paraply(resultat: Vilkårsresultat): Vilkår =
        vilkår(behandlingId = behandlingId, type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE, resultat = resultat)

    private fun reise(
        paraply: Vilkår,
        resultat: Vilkårsresultat,
    ): Vilkår =
        vilkår(behandlingId = behandlingId, type = VilkårType.ROAH_OPPSTART_AVSLUTNING, resultat = resultat)
            .copy(parentVilkår = paraply.globalId)
}
