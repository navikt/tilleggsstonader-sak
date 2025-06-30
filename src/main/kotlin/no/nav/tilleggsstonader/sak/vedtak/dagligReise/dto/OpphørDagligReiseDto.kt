package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

data class OpphørDagligReiseResponse(
    val vedtaksperioder: List<VedtaksperiodeDagligReiseDto>,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakDagligReiseDto(TypeVedtak.OPPHØR),
    VedtakDagligReiseResponse

data class OpphørDagligReiseRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakDagligReiseDto(TypeVedtak.OPPHØR),
    VedtakDagligReiseRequest
