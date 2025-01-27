package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode

enum class TypeVedtakLæremidler(override val typeVedtak: TypeVedtak) : TypeVedtaksdata {
    INNVILGELSE_LÆREMIDLER(TypeVedtak.INNVILGELSE),
    AVSLAG_LÆREMIDLER(TypeVedtak.AVSLAG),
    OPPHØR_LÆREMIDLER(TypeVedtak.OPPHØR),
}

sealed interface VedtakLæremidler : Vedtaksdata

sealed interface InnvilgelseEllerOpphørLæremidler : VedtakLæremidler {
    val vedtaksperioder: List<Vedtaksperiode>
    val beregningsresultat: BeregningsresultatLæremidler
}

data class InnvilgelseLæremidler(
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsresultat: BeregningsresultatLæremidler,
) : InnvilgelseEllerOpphørLæremidler, Innvilgelse {
    override val type: TypeVedtaksdata = TypeVedtakLæremidler.INNVILGELSE_LÆREMIDLER
}

data class AvslagLæremidler(
    override val årsaker: List<ÅrsakAvslag>,
    override val begrunnelse: String,
) : VedtakLæremidler, Avslag {

    override val type: TypeVedtaksdata = TypeVedtakLæremidler.AVSLAG_LÆREMIDLER

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

data class OpphørLæremidler(
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsresultat: BeregningsresultatLæremidler,
    override val årsaker: List<ÅrsakOpphør>,
    override val begrunnelse: String,
) : InnvilgelseEllerOpphørLæremidler, Opphør {

    override val type: TypeVedtaksdata = TypeVedtakLæremidler.OPPHØR_LÆREMIDLER

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

fun VedtakLæremidler.beregningsresultat(): BeregningsresultatLæremidler? {
    return when (this) {
        is InnvilgelseLæremidler -> this.beregningsresultat
        is OpphørLæremidler -> this.beregningsresultat
        is AvslagLæremidler -> null
    }
}

fun VedtakLæremidler.vedtaksperioder(): List<Vedtaksperiode>? {
    return when (this) {
        is InnvilgelseLæremidler -> this.vedtaksperioder
        is OpphørLæremidler -> this.vedtaksperioder
        is AvslagLæremidler -> null
    }
}
