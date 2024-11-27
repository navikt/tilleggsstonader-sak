package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

data class AvslagTilsynBarnDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakTilsynBarnRequest, VedtakTilsynBarnResponse, VedtakTilsynBarnDto(TypeVedtak.AVSLAG)
