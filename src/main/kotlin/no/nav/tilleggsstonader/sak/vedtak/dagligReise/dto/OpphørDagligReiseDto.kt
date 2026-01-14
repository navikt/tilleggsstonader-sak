package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import java.time.LocalDate

data class OpphørDagligReiseResponse(
    val beregningsresultat: BeregningsresultatDagligReiseDto,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val opphørsdato: LocalDate?,
) : VedtakDagligReiseDto(TypeVedtak.OPPHØR),
    VedtakDagligReiseResponse

data class OpphørDagligReiseRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
    val opphørsdato: LocalDate?,
) : VedtakDagligReiseDto(TypeVedtak.OPPHØR),
    VedtakDagligReiseRequest
