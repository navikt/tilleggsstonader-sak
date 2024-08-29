package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import java.util.UUID

data class SvarPåVilkårDto(
    val id: UUID,
    val behandlingId: UUID,
    val delvilkårsett: List<DelvilkårDto>,
)
