package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn

enum class TypeVedtakTilsynBarn(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    INNVILGELSE_TILSYN_BARN(TypeVedtak.INNVILGELSE),
    AVSLAG_TILSYN_BARN(TypeVedtak.AVSLAG),
    OPPHØR_TILSYN_BARN(TypeVedtak.OPPHØR),
}

sealed interface VedtakTilsynBarn : Vedtaksdata

data class InnvilgelseTilsynBarn(
    val beregningsresultat: BeregningsresultatTilsynBarn,
    val vedtaksperioder: List<VedtaksperiodeBeregning>? = null,
) : VedtakTilsynBarn,
    Innvilgelse {
    override val type: TypeVedtaksdata = TypeVedtakTilsynBarn.INNVILGELSE_TILSYN_BARN
}

data class AvslagTilsynBarn(
    override val årsaker: List<ÅrsakAvslag>,
    override val begrunnelse: String,
) : VedtakTilsynBarn,
    Avslag {
    override val type: TypeVedtaksdata = TypeVedtakTilsynBarn.AVSLAG_TILSYN_BARN

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

data class OpphørTilsynBarn(
    val beregningsresultat: BeregningsresultatTilsynBarn,
    override val årsaker: List<ÅrsakOpphør>,
    override val begrunnelse: String,
    val vedtaksperioder: List<VedtaksperiodeBeregning>? = null,
) : VedtakTilsynBarn,
    Opphør {
    override val type: TypeVedtaksdata = TypeVedtakTilsynBarn.OPPHØR_TILSYN_BARN

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

fun VedtakTilsynBarn.beregningsresultat(): BeregningsresultatTilsynBarn? =
    when (this) {
        is InnvilgelseTilsynBarn -> this.beregningsresultat
        is OpphørTilsynBarn -> this.beregningsresultat
        is AvslagTilsynBarn -> null
    }
