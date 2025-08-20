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

sealed interface InnvilgelseEllerOpphørTilsynBarn : VedtakTilsynBarn {
    val beregningsresultat: BeregningsresultatTilsynBarn
    val vedtaksperioder: List<Vedtaksperiode>
}

data class InnvilgelseTilsynBarn(
    override val beregningsresultat: BeregningsresultatTilsynBarn,
    override val vedtaksperioder: List<Vedtaksperiode>,
    val begrunnelse: String? = null,
) : InnvilgelseEllerOpphørTilsynBarn,
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
    override val beregningsresultat: BeregningsresultatTilsynBarn,
    override val årsaker: List<ÅrsakOpphør>,
    override val begrunnelse: String,
    override val vedtaksperioder: List<Vedtaksperiode>,
) : InnvilgelseEllerOpphørTilsynBarn,
    Opphør {
    override val type: TypeVedtaksdata = TypeVedtakTilsynBarn.OPPHØR_TILSYN_BARN

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}
