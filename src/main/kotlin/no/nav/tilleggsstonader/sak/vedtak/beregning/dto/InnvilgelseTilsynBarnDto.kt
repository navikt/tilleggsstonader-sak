package no.nav.tilleggsstonader.sak.vedtak.beregning.dto

import java.util.UUID

data class InnvilgelseTilsynBarnDto(
    val behandlingId: UUID,
    val perioder: List<String>
)

data class BeløpsperioderTilsynBarnDto(
    val periode: List<String>
)