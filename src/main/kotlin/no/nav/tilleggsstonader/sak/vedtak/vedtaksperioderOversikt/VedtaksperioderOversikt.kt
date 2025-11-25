package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperiodeBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReiseTso
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperiodeDagligReiseTsr
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.DetaljertVedtaksperiodeLæremidler

data class VedtaksperioderOversikt(
    val tilsynBarn: VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeTilsynBarn>?,
    val læremidler: VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeLæremidler>?,
    val boutgifter: VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeBoutgifter>?,
    val dagligReiseTso: VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeDagligReiseTso>?,
    val dagligReiseTsr: VedtaksperiodeOversiktForBehandling<DetaljertVedtaksperiodeDagligReiseTsr>?,
)

data class VedtaksperiodeOversiktForBehandling<T : DetaljertVedtaksperiode>(
    val detaljerteVedtaksperioder: List<T>,
    val vedtaksperioder: List<Datoperiode>,
)
