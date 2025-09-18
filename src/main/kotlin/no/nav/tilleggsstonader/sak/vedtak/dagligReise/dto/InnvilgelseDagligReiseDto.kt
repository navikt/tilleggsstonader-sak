package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.time.LocalDate

/**
 * Beregningsresultatet kan være en del av en trettidagers periode - Eks 1jan-30jan
 */
data class InnvilgelseDagligReiseResponse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val beregningsresultat: BeregningsresultatDagligReise,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
    val begrunnelse: String? = null,
) : VedtakDagligReiseDto(TypeVedtak.INNVILGELSE),
    VedtakDagligReiseResponse

data class InnvilgelseDagligReiseRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val begrunnelse: String? = null,
) : VedtakDagligReiseRequest
