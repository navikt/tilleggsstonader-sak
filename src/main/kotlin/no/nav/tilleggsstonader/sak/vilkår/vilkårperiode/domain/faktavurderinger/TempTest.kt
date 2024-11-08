package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.GeneriskVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import java.time.LocalDate
import java.util.UUID

fun main() {
    val lønnet = DelvilkårVilkårperiode.Vurdering(SvarJaNei.NEI, resultat = ResultatDelvilkårperiode.OPPFYLT)
    val list = listOf(
        GeneriskVilkårperiode<FaktaOgVurdering>(
            id = UUID.randomUUID(),
            behandlingId = BehandlingId.random(),
            resultat = ResultatVilkårperiode.IKKE_VURDERT,
            faktaOgVurdering = TiltakTilsynBarn(
                fakta = FaktaAktivitetTilsynBarn(
                    5,
                ),
                vurderinger = VurderingTiltakTilsynBarn(
                    lønnet = lønnet,
                ),
            ),
            type = AktivitetType.TILTAK,
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            begrunnelse = "",
        ),
    )

    println(list.ofType<TiltakTilsynBarn>())
    println(list.ofType<UtdanningTilsynBarn>())
    println(list.ofType<MålgruppeTilsynBarn>())
}
