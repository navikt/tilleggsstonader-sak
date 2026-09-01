package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling

enum class TypeVedtakReiseTilSamling(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    INNVILGELSE_REISE_TIL_SAMLING(TypeVedtak.INNVILGELSE),
    //  AVSLAG_REISE_TIL_SAMLING(TypeVedtak.AVSLAG),
    //  OPPHØR_REISE_TIL_SAMLING(TypeVedtak.OPPHØR),
}

enum class TypeReiseTilSamling {
    OFFENTLIG_TRANSPORT,
    PRIVAT_BIL,
    UBESTEMT,
}

sealed interface VedtakReiseTilSamling : Vedtaksdata

sealed interface InnvilgelseEllerOpphørReiseTilSamling : VedtakReiseTilSamling {
    val beregningsresultat: BeregningsresultatReiseTilSamling
    val vedtaksperioder: List<Vedtaksperiode>
    val beregningsplan: Beregningsplan
}

data class InnvilgelseReiseTilSamling(
    override val beregningsresultat: BeregningsresultatReiseTilSamling,
    override val vedtaksperioder: List<Vedtaksperiode>,
    val begrunnelse: String? = null,
    override val beregningsplan: Beregningsplan,
) : InnvilgelseEllerOpphørReiseTilSamling,
    Innvilgelse {
    override val type: TypeVedtaksdata =
        TypeVedtakReiseTilSamling
            .INNVILGELSE_REISE_TIL_SAMLING
}
