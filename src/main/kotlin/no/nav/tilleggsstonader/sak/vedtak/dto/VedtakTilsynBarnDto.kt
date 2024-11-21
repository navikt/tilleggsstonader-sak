package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

sealed interface VedtakTilsynBarnDto : VedtakDto

enum class TilsynBarnVedtakType: VedtakType {
    INNVILGELSE_TILSYN_BARN,
    OPPHØR_TILSYN_BARN,
    AVSLAG_TILSYN_BARN,
}

data object InnvilgelseTilsynBarnRequest : VedtakTilsynBarnDto {
    override val vedtakType: TilsynBarnVedtakType = TilsynBarnVedtakType.INNVILGELSE_TILSYN_BARN
}

data class InnvilgelseTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto
) : VedtakTilsynBarnDto {
    override val vedtakType: TilsynBarnVedtakType = TilsynBarnVedtakType.INNVILGELSE_TILSYN_BARN
}

data class AvslagTilsynBarnDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakTilsynBarnDto {
    override val vedtakType: TilsynBarnVedtakType = TilsynBarnVedtakType.AVSLAG_TILSYN_BARN
}

data class OpphørDto(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
): VedtakTilsynBarnDto {
    override val vedtakType: TilsynBarnVedtakType = TilsynBarnVedtakType.OPPHØR_TILSYN_BARN
}