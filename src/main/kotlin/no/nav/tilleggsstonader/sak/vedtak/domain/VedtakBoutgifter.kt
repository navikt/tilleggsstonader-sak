package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType

enum class TypeVedtakBoutgifter(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    INNVILGELSE_BOUTGIFTER(TypeVedtak.INNVILGELSE),
    AVSLAG_BOUTGIFTER(TypeVedtak.AVSLAG),
    OPPHØR_BOUTGIFTER(TypeVedtak.OPPHØR),
}

enum class TypeBoutgift {
    UTGIFTER_OVERNATTING,
    LØPENDE_UTGIFTER_EN_BOLIG,
    LØPENDE_UTGIFTER_TO_BOLIGER,
    ;

    companion object {
        fun fraVilkårType(vilkårType: VilkårType) =
            when (vilkårType) {
                VilkårType.UTGIFTER_OVERNATTING -> UTGIFTER_OVERNATTING
                VilkårType.LØPENDE_UTGIFTER_EN_BOLIG -> LØPENDE_UTGIFTER_EN_BOLIG
                VilkårType.LØPENDE_UTGIFTER_TO_BOLIGER -> LØPENDE_UTGIFTER_TO_BOLIGER
                else -> error("$vilkårType er ikke en gyldig utgiftstype for boutgifter")
            }
    }
}

sealed interface VedtakBoutgifter : Vedtaksdata

sealed interface InnvilgelseEllerOpphørBoutgifter : VedtakBoutgifter {
    val beregningsresultat: BeregningsresultatBoutgifter
    val vedtaksperioder: List<Vedtaksperiode>
}

data class InnvilgelseBoutgifter(
    override val beregningsresultat: BeregningsresultatBoutgifter,
    override val vedtaksperioder: List<Vedtaksperiode>,
    val begrunnelse: String? = null,
) : InnvilgelseEllerOpphørBoutgifter,
    Innvilgelse {
    override val type: TypeVedtaksdata = TypeVedtakBoutgifter.INNVILGELSE_BOUTGIFTER
}

data class AvslagBoutgifter(
    override val årsaker: List<ÅrsakAvslag>,
    override val begrunnelse: String,
) : VedtakBoutgifter,
    Avslag {
    override val type: TypeVedtaksdata = TypeVedtakBoutgifter.AVSLAG_BOUTGIFTER

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}

data class OpphørBoutgifter(
    override val vedtaksperioder: List<Vedtaksperiode>,
    override val beregningsresultat: BeregningsresultatBoutgifter,
    override val årsaker: List<ÅrsakOpphør>,
    override val begrunnelse: String,
) : InnvilgelseEllerOpphørBoutgifter,
    Opphør {
    override val type: TypeVedtaksdata = TypeVedtakBoutgifter.OPPHØR_BOUTGIFTER

    init {
        this.validerÅrsakerOgBegrunnelse()
    }
}
