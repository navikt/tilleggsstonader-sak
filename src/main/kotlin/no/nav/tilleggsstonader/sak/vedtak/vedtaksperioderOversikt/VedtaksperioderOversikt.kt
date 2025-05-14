package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperiodeBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.DetaljertVedtaksperiodeLæremidler

data class VedtaksperioderOversikt(
    val tilsynBarn: List<DetaljertVedtaksperiodeTilsynBarn>,
    val læremidler: List<DetaljertVedtaksperiodeLæremidler>,
    val boutgifter: List<DetaljertVedtaksperiodeBoutgifter>,
)
