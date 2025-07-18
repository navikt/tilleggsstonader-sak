package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import java.time.LocalDate

data class OpphørLæremidlerResponse(
    val vedtaksperioder: List<VedtaksperiodeLæremidlerDto>,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakLæremidlerDto(TypeVedtak.OPPHØR),
    VedtakLæremidlerResponse

data class OpphørLæremidlerRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakLæremidlerDto(TypeVedtak.OPPHØR),
    VedtakLæremidlerRequest
