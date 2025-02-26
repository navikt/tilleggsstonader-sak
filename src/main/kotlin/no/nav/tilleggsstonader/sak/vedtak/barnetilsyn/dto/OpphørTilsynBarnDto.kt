package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto

data class OpphørTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val vedtaksperioder: List<VedtaksperiodeDto>?,
) : VedtakTilsynBarnDto(TypeVedtak.OPPHØR),
    VedtakTilsynBarnResponse

data class OpphørTilsynBarnRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakTilsynBarnDto(TypeVedtak.OPPHØR),
    VedtakTilsynBarnRequest
