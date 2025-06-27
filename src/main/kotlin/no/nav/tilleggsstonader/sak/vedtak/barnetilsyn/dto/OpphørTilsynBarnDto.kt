package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.time.LocalDate

data class OpphørTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val vedtaksperioder: List<VedtaksperiodeDto>?,
    val opphørsdato: LocalDate?,
) : VedtakTilsynBarnDto(TypeVedtak.OPPHØR),
    VedtakTilsynBarnResponse

data class OpphørTilsynBarnRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakTilsynBarnDto(TypeVedtak.OPPHØR),
    VedtakTilsynBarnRequest
