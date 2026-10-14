package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting

enum class TypeVedtakFlytting(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    INNVILGELSE_FLYTTING(TypeVedtak.INNVILGELSE),
    AVSLAG_FLYTTING(TypeVedtak.AVSLAG),
    OPPHØR_FLYTTING(TypeVedtak.OPPHØR),
}

sealed interface VedtakFlytting : Vedtaksdata

sealed interface InnvilgelseEllerOpphørFlytting : VedtakFlytting {
    val vedtaksperioder: List<Vedtaksperiode>
    val beregningsplan: Beregningsplan
    val beregningsresultat: BeregningsresultatFlytting
}

data class InnvilgelseFlytting(
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsplan: Beregningsplan,
    override val beregningsresultat: BeregningsresultatFlytting,
    val begrunnelse: String? = null,
) : InnvilgelseEllerOpphørFlytting,
    Innvilgelse {
    override val type: TypeVedtaksdata = TypeVedtakFlytting.INNVILGELSE_FLYTTING
}

data class OpphørFlytting(
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsplan: Beregningsplan,
    override val beregningsresultat: BeregningsresultatFlytting,
    override val årsaker: List<ÅrsakOpphør>,
    override val begrunnelse: String,
) : InnvilgelseEllerOpphørFlytting,
    Opphør {
    override val type: TypeVedtaksdata = TypeVedtakFlytting.OPPHØR_FLYTTING

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

data class AvslagFlytting(
    override val årsaker: List<ÅrsakAvslag>,
    override val begrunnelse: String,
) : VedtakFlytting,
    Avslag {
    override val type: TypeVedtaksdata = TypeVedtakFlytting.AVSLAG_FLYTTING

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}
