package no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import java.time.LocalDate

data class OpphørPassAvBarnResponse(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val opphørsdato: LocalDate,
) : VedtakPassAvBarnDto(TypeVedtak.OPPHØR),
    VedtakPassAvBarnResponse

data class OpphørPassAvBarnRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakPassAvBarnDto(TypeVedtak.OPPHØR),
    VedtakPassAvBarnRequest
