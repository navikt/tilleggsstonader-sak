package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt.boutgifter.DetaljertVedtaksperiodeBoutgifterV2

data class VedtaksperioderOversikt(
    val tilsynBarn: List<DetaljertVedtaksperiodeTilsynBarn>,
    val læremidler: List<DetaljertVedtaksperiodeLæremidler>,
    val boutgifter: List<DetaljertVedtaksperiodeBoutgifterV2>,
)
