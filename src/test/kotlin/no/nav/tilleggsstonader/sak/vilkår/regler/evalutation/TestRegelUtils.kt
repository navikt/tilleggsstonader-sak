package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto

fun delvilkårsvurderingDto(vararg vurderinger: VurderingDto) =
    DelvilkårsvurderingDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())
