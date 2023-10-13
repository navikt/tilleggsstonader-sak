package no.nav.tilleggsstonader.sak.vedtak.beregning.barnetilsyn

import java.util.UUID

data class InnvilgelseTilsynBarnDto(
    val behandlingId: UUID,
    val perioder: List<String>
)

data class BeregningsgrunnlagTilsynBarnDto(
    val perioder: List<String>
)

data class BeregningsresultatTilsynBarnDto(
    val periode: List<String>
)