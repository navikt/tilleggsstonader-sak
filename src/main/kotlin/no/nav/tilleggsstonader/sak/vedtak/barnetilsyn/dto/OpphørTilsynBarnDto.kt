package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

data class OpphørTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakTilsynBarnDto(TypeVedtak.OPPHØR),
    VedtakTilsynBarnResponse

data class OpphørTilsynBarnRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakTilsynBarnDto(TypeVedtak.OPPHØR),
    VedtakTilsynBarnRequest
