package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType

enum class TypeVedtakDagligReise(
    override val typeVedtak: TypeVedtak,
) : TypeVedtaksdata {
    INNVILGELSE_DAGLIG_REISE(TypeVedtak.INNVILGELSE),
    AVSLAG_DAGLIG_REISE(TypeVedtak.AVSLAG),
    OPPHØR_DAGLIG_REISE(TypeVedtak.OPPHØR),
}

enum class TypeDagligReise {
    OFFENTLIG_TRANSPORT,
    KJØRELISTE,
    TAXI,
    ;

    fun tilVilkårType() =
        when (this) {
            OFFENTLIG_TRANSPORT -> VilkårType.DAGLIG_REISE_OFFENTLIG_TRANSPORT
            KJØRELISTE -> VilkårType.DAGLIG_REISE_KJØRELISTE
            TAXI -> VilkårType.DAGLIG_REISE_TAXI
        }

    companion object {
        fun fraVilkårType(vilkårType: VilkårType) =
            when (vilkårType) {
                VilkårType.DAGLIG_REISE_OFFENTLIG_TRANSPORT -> OFFENTLIG_TRANSPORT
                VilkårType.DAGLIG_REISE_KJØRELISTE -> KJØRELISTE
                VilkårType.DAGLIG_REISE_TAXI -> TAXI
                else -> error("$vilkårType er ikke et gyldig vilkår for daglig reise")
            }
    }
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
