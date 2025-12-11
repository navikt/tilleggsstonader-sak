package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import java.time.LocalDate

data class OpphørDagligReiseResponse(
    val beregningsresultat: BeregningsresultatDagligReiseDto, // TODO skal dettte være BeregningsresultatDagligReise ?
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
