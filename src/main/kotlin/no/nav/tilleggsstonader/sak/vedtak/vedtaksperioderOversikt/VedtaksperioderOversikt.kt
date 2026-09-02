package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperiodeBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.DetaljertVedtaksperiodeLæremidler
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.detaljerteVedtaksperioder.DetaljertVedtaksperiodePassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.detaljerteVedtaksperioder.DetaljertVedtaksperiodeReiseTilSamling

data class VedtaksperioderOversikt(
    // TODO TilsynBarn bør hete PassAvBarn, men brukes eksternt
    val tilsynBarn: List<DetaljertVedtaksperiodePassAvBarn>,
    val læremidler: List<DetaljertVedtaksperiodeLæremidler>,
    val boutgifter: List<DetaljertVedtaksperiodeBoutgifter>,
    val dagligReiseTso: List<DetaljertVedtaksperiodeDagligReise>,
    val dagligReiseTsr: List<DetaljertVedtaksperiodeDagligReise>,
    val reiseTilSamlingTso: List<DetaljertVedtaksperiodeReiseTilSamling>,
    val reiseTilSamlingTsr: List<DetaljertVedtaksperiodeReiseTilSamling>,
)
