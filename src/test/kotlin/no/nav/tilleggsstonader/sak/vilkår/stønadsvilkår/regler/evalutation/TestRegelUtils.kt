package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VurderingDto

fun delvilkårDto(vararg vurderinger: VurderingDto) =
    DelvilkårDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())
