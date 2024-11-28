package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler

enum class TypeVedtakLæremidler(override val typeVedtak: TypeVedtak) : TypeVedtaksdata {
    // INNVILGELSE_LÆREMIDLER(TypeVedtak.INNVILGELSE),
    AVSLAG_LÆREMIDLER(TypeVedtak.AVSLAG),
    // OPPHØR_LÆREMIDLER(TypeVedtak.OPPHØR),
}

sealed interface VedtakLæremidler : Vedtaksdata

/*
data class InnvilgelseLæremidler(
    val beregningsresultat: BeregningsresultatLæremidler,
) : VedtakLæremidler {
    override val type: TypeVedtaksdata = TypeVedtakLæremidler.INNVILGELSE_LÆREMIDLER
}*/

data class AvslagLæremidler(
    val årsaker: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakLæremidler {

    override val type: TypeVedtaksdata = TypeVedtakLæremidler.AVSLAG_LÆREMIDLER

    init {
        require(årsaker.isNotEmpty()) { "Må velge minst en årsak for avslag" }
        require(begrunnelse.isNotBlank()) { "Avslag må begrunnes" }
    }
}

/*
data class OpphørLæremidler(
    val beregningsresultat: BeregningsresultatLæremidler,
    val årsaker: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakLæremidler {

    override val type: TypeVedtaksdata = TypeVedtakLæremidler.OPPHØR_LÆREMIDLER

    init {
        require(årsaker.isNotEmpty()) { "Må velge minst en årsak for opphør" }
        require(begrunnelse.isNotBlank()) { "Opphør må begrunnes" }
    }
}
*/
fun VedtakLæremidler.beregningsresultat(): BeregningsresultatLæremidler? {
    return when (this) {
        // is InnvilgelseLæremidler -> this.beregningsresultat
        // is OpphørLæremidler -> this.beregningsresultat
        is AvslagLæremidler -> null
    }
}
