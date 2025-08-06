package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.time.LocalDate

data class OpphørBoutgifterResponse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakBoutgifterDto(TypeVedtak.OPPHØR),
    VedtakBoutgifterResponse

data class OpphørBoutgifterRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakBoutgifterDto(TypeVedtak.OPPHØR),
    VedtakBoutgifterRequest
