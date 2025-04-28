package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto

sealed class OppsummertVedtak(
    val resultat: TypeVedtak,
)

data class OppsummertVedtakInnvilgelse(
    val vedtaksperioder: List<VedtaksperiodeDto>,
) : OppsummertVedtak(resultat = TypeVedtak.INNVILGELSE)

data class OppsummertVedtakAvslag(
    val årsaker: List<ÅrsakAvslag>,
) : OppsummertVedtak(resultat = TypeVedtak.AVSLAG)

object OppsummertVedtakOpphør : OppsummertVedtak(resultat = TypeVedtak.OPPHØR)
