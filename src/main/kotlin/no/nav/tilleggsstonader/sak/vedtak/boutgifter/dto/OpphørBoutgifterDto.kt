package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import java.time.LocalDate

data class OpphørBoutgifterResponse(
    val vedtaksperioder: List<VedtaksperiodeBoutgifterDto>,
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
