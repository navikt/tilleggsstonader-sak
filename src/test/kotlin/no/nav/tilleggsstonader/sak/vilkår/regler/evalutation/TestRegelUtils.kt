package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto

fun delvilkårDto(vararg vurderinger: VurderingDto) =
    DelvilkårDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())
