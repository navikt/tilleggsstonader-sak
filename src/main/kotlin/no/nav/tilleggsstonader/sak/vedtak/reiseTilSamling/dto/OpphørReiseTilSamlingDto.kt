package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import java.time.LocalDate

data class OpphørReiseTilSamlingResponse(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val opphørsdato: LocalDate,
) : VedtakReiseTilSamlingDto(TypeVedtak.OPPHØR),
    VedtakReiseTilSamlingResponse

data class OpphørReiseTilSamlingRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakReiseTilSamlingDto(TypeVedtak.OPPHØR),
    VedtakReiseTilSamlingRequest
