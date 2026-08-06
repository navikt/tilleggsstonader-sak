package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn

enum class TypeVedtakPassAvBarn(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    // Beholder enums som tilsyn_barn, da disse kanskje brukes andre steder
    INNVILGELSE_TILSYN_BARN(TypeVedtak.INNVILGELSE),
    AVSLAG_TILSYN_BARN(TypeVedtak.AVSLAG),
    OPPHØR_TILSYN_BARN(TypeVedtak.OPPHØR),
}

sealed interface VedtakPassAvBarn : Vedtaksdata

sealed interface InnvilgelseEllerOpphørPassAvBarn : VedtakPassAvBarn {
    val beregningsresultat: BeregningsresultatPassAvBarn
    val vedtaksperioder: List<Vedtaksperiode>
    val beregningsplan: Beregningsplan
}

data class InnvilgelsePassAvBarn(
    override val beregningsresultat: BeregningsresultatPassAvBarn,
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsplan: Beregningsplan,
    val begrunnelse: String? = null,
) : InnvilgelseEllerOpphørPassAvBarn,
    Innvilgelse {
    override val type: TypeVedtaksdata = TypeVedtakPassAvBarn.INNVILGELSE_TILSYN_BARN
}

data class AvslagPassAvBarn(
    override val årsaker: List<ÅrsakAvslag>,
    override val begrunnelse: String,
) : VedtakPassAvBarn,
    Avslag {
    override val type: TypeVedtaksdata = TypeVedtakPassAvBarn.AVSLAG_TILSYN_BARN

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

data class OpphørPassAvBarn(
    override val beregningsresultat: BeregningsresultatPassAvBarn,
    override val årsaker: List<ÅrsakOpphør>,
    override val begrunnelse: String,
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsplan: Beregningsplan,
) : InnvilgelseEllerOpphørPassAvBarn,
    Opphør {
    override val type: TypeVedtaksdata = TypeVedtakPassAvBarn.OPPHØR_TILSYN_BARN

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}
