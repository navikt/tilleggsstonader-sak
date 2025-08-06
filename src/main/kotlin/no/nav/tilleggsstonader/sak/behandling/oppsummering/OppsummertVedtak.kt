package no.nav.tilleggsstonader.sak.behandling.oppsummering

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import java.time.LocalDate

sealed class OppsummertVedtak(
    val resultat: TypeVedtak,
)

data class OppsummertVedtakInnvilgelse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>,
) : OppsummertVedtak(resultat = TypeVedtak.INNVILGELSE)

data class OppsummertVedtakAvslag(
    val årsaker: List<ÅrsakAvslag>,
) : OppsummertVedtak(resultat = TypeVedtak.AVSLAG)

data class OppsummertVedtakOpphør(
    val årsaker: List<ÅrsakOpphør>,
    val opphørsdato: LocalDate,
) : OppsummertVedtak(resultat = TypeVedtak.OPPHØR)
