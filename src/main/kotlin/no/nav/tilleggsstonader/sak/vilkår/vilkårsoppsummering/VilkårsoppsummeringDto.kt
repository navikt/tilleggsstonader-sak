package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto

data class VilkårsoppsummeringDto(
    val stønadsperioder: List<StønadsperiodeDto>,
    val visVarselKontantstøtte: Boolean,
)
