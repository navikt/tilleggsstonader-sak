package no.nav.tilleggsstonader.sak.vedtak.felles

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtaksdataTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.ÅrsakAvslag

sealed interface VedtakTilsynBarn : Vedtak

data class InnvilgelseTilsynBarn(
    val vedtak: VedtaksdataTilsynBarn? = null,
    val beregningsresultat: BeregningsresultatTilsynBarn? = null,
) : VedtakTilsynBarn

data class AvslagTilsynBarn(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
): VedtakTilsynBarn