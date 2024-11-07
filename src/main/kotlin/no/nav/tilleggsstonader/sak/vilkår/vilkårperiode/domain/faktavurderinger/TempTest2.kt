package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei

fun main() {
    val målgruppe = MålgruppeTilsynBarn(
        type = MålgruppeTilsynBarnType.AAP_TILSYN_BARN,
        vurderinger = MålgruppeVurderinger(
            medlemskap = DelvilkårVilkårperiode.Vurdering(SvarJaNei.JA, ResultatDelvilkårperiode.OPPFYLT),
            dekketAvAnnetRegelverk = DelvilkårVilkårperiode.Vurdering(SvarJaNei.NEI, ResultatDelvilkårperiode.OPPFYLT),
        ),
    )
    val json = objectMapper.writeValueAsString(målgruppe)
    val faktaOgVurdering = objectMapper.readValue<FaktaOgVurdering>(json)
    println(json)
    println(faktaOgVurdering)
    println(faktaOgVurdering is MålgruppeTilsynBarn)
    println(faktaOgVurdering is AktivitetTilsynBarn)
}
