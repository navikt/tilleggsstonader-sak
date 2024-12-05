package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn

enum class TypeVedtakTilsynBarn(override val typeVedtak: TypeVedtak) : TypeVedtaksdata {
    INNVILGELSE_TILSYN_BARN(TypeVedtak.INNVILGELSE),
    AVSLAG_TILSYN_BARN(TypeVedtak.AVSLAG),
    OPPHØR_TILSYN_BARN(TypeVedtak.OPPHØR),
}

sealed interface VedtakTilsynBarn : Vedtaksdata

data class InnvilgelseTilsynBarn(
    val beregningsresultat: BeregningsresultatTilsynBarn,
) : VedtakTilsynBarn {
    override val type: TypeVedtaksdata = TypeVedtakTilsynBarn.INNVILGELSE_TILSYN_BARN
}

data class AvslagTilsynBarn(
    val årsaker: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakTilsynBarn {

    override val type: TypeVedtaksdata = TypeVedtakTilsynBarn.AVSLAG_TILSYN_BARN

    init {
        require(årsaker.isNotEmpty()) { "Må velge minst en årsak for avslag" }
        require(begrunnelse.isNotBlank()) { "Avslag må begrunnes" }
    }
}

data class OpphørTilsynBarn(
    val beregningsresultat: BeregningsresultatTilsynBarn,
    val årsaker: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakTilsynBarn {

    override val type: TypeVedtaksdata = TypeVedtakTilsynBarn.OPPHØR_TILSYN_BARN

    init {
        require(årsaker.isNotEmpty()) { "Må velge minst en årsak for opphør" }
        require(begrunnelse.isNotBlank()) { "Opphør må begrunnes" }
    }
}

fun VedtakTilsynBarn.beregningsresultat(): BeregningsresultatTilsynBarn? {
    return when (this) {
        is InnvilgelseTilsynBarn -> this.beregningsresultat
        is OpphørTilsynBarn -> this.beregningsresultat
        is AvslagTilsynBarn -> null
    }
}
