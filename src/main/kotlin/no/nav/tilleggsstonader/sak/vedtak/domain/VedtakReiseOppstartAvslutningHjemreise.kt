package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningReiseOppstartAvslutningHjemreise

enum class TypeVedtakReiseOppstartAvslutningHjemreise(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    INNVILGELSE_REISE_OPPSTART_AVSLUTNING_HJEMREISE(TypeVedtak.INNVILGELSE),
    //  AVSLAG_REISE_OPPSTART_AVSLUTNING_HJEMREISE(TypeVedtak.AVSLAG),
    //  OPPHØR_REISE_OPPSTART_AVSLUTNING_HJEMREISE(TypeVedtak.OPPHØR),
}

sealed interface VedtakReiseOppstartAvslutningHjemreise : Vedtaksdata

sealed interface InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise : VedtakReiseOppstartAvslutningHjemreise {
    val beregningsresultat: BeregningReiseOppstartAvslutningHjemreise
    val vedtaksperioder: List<Vedtaksperiode>
    val beregningsplan: Beregningsplan
}

data class InnvilgelseReiseOppstartAvslutningHjemreise(
    override val beregningsresultat: BeregningReiseOppstartAvslutningHjemreise,
    override val vedtaksperioder: List<Vedtaksperiode>,
    val begrunnelse: String? = null,
    override val beregningsplan: Beregningsplan,
) : InnvilgelseEllerOpphørReiseOppstartAvslutningHjemreise,
    Innvilgelse {
    override val type: TypeVedtaksdata =
        TypeVedtakReiseOppstartAvslutningHjemreise
            .INNVILGELSE_REISE_OPPSTART_AVSLUTNING_HJEMREISE
}
