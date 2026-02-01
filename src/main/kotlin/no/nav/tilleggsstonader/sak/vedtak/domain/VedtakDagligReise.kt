package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise

enum class TypeVedtakDagligReise(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    INNVILGELSE_DAGLIG_REISE(TypeVedtak.INNVILGELSE),
    AVSLAG_DAGLIG_REISE(TypeVedtak.AVSLAG),
    OPPHØR_DAGLIG_REISE(TypeVedtak.OPPHØR),
}

enum class TypeDagligReise {
    OFFENTLIG_TRANSPORT,
    PRIVAT_BIL,
    TAXI,
    UBESTEMT,
}

sealed interface VedtakDagligReise : Vedtaksdata

sealed interface InnvilgelseEllerOpphørDagligReise : VedtakDagligReise {
    val beregningsresultat: BeregningsresultatDagligReise
    val vedtaksperioder: List<Vedtaksperiode>
}

data class InnvilgelseDagligReise(
    override val beregningsresultat: BeregningsresultatDagligReise,
    override val vedtaksperioder: List<Vedtaksperiode>,
    val begrunnelse: String? = null,
) : InnvilgelseEllerOpphørDagligReise,
    Innvilgelse {
    override val type: TypeVedtaksdata =
        TypeVedtakDagligReise
            .INNVILGELSE_DAGLIG_REISE
}

data class AvslagDagligReise(
    override val årsaker: List<ÅrsakAvslag>,
    override val begrunnelse: String,
) : VedtakDagligReise,
    Avslag {
    override val type: TypeVedtaksdata =
        TypeVedtakDagligReise
            .AVSLAG_DAGLIG_REISE

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

data class OpphørDagligReise(
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsresultat: BeregningsresultatDagligReise,
    override val årsaker: List<ÅrsakOpphør>,
    override val begrunnelse: String,
) : InnvilgelseEllerOpphørDagligReise,
    Opphør {
    override val type: TypeVedtaksdata =
        TypeVedtakDagligReise
            .OPPHØR_DAGLIG_REISE

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}
