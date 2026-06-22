package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.time.LocalDate

data class InnvilgelseReiseTilSamlingResponse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val beregningsresultat: BeregningsResultatReiseTilSamlingDto,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
    val begrunnelse: String? = null,
) : VedtakReiseTilSamlingDto(TypeVedtak.INNVILGELSE),
    VedtakReiseTilSamlingResponse

data class InnvilgelseReiseTilSamlingTsoRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val begrunnelse: String? = null,
) : VedtakReiseTilSamlingRequest
