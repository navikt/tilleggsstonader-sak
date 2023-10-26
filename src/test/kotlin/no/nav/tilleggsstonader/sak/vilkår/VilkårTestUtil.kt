package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårWrapper
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import java.util.UUID

object VilkårTestUtil {

    fun lagOppfyltMålgruppeRegel(behandlingId: UUID = UUID.randomUUID()) = Vilkår(
        behandlingId = behandlingId,
        type = VilkårType.MÅLGRUPPE,
        resultat = Vilkårsresultat.OPPFYLT,
        delvilkårwrapper = DelvilkårWrapper(
            listOf(
                Delvilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    vurderinger = listOf(Vurdering(RegelId.MÅLGRUPPE, svar = SvarId.JA)),
                ),
            ),
        ),
        opphavsvilkår = null,
    )

    fun lagOppfyltAktivitetRegel(behandlingId: UUID = UUID.randomUUID()) = Vilkår(
        behandlingId = behandlingId,
        type = VilkårType.AKTIVITET,
        resultat = Vilkårsresultat.OPPFYLT,
        delvilkårwrapper = DelvilkårWrapper(
            listOf(
                Delvilkår(
                    resultat = Vilkårsresultat.OPPFYLT,
                    vurderinger = listOf(Vurdering(RegelId.ER_AKTIVITET_REGISTRERT, svar = SvarId.JA)),
                ),
            ),
        ),
        opphavsvilkår = null,
    )
}
