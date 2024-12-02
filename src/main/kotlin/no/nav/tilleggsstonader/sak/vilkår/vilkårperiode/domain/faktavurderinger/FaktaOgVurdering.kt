package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType

/**
 * Felles interface for Fakta og Vurderinger
 */
sealed interface FaktaOgVurdering : FaktaOgVurderingJson {
    val type: TypeFaktaOgVurdering
    val fakta: Fakta
    val vurderinger: Vurderinger

    fun utledResultat(): ResultatVilkårperiode {
        if (type.vilkårperiodeType.girIkkeRettPåStønadsperiode()) {
            return ResultatVilkårperiode.IKKE_OPPFYLT
        }
        return this.utledResultat(this.vurderinger.utledDelresultater())
    }

    private fun utledResultat(resultater: List<ResultatDelvilkårperiode>) =
        when {
            resultater.contains(ResultatDelvilkårperiode.IKKE_VURDERT) -> {
                ResultatVilkårperiode.IKKE_VURDERT
            }

            resultater.contains(ResultatDelvilkårperiode.IKKE_OPPFYLT) -> {
                ResultatVilkårperiode.IKKE_OPPFYLT
            }

            resultater.all { it == ResultatDelvilkårperiode.OPPFYLT } -> {
                ResultatVilkårperiode.OPPFYLT
            }

            else -> {
                error("Ugyldig resultat ($resultater)")
            }
        }
}

sealed interface MålgruppeFaktaOgVurdering : FaktaOgVurdering
sealed interface AktivitetFaktaOgVurdering : FaktaOgVurdering

/**
 * Typer, som implementers av enums for å få unike typer på hvert objekt
 * for å kunne deserialisere til riktig objekt
 * Eks [AktivitetTilsynBarnType.TILTAK_TILSYN_BARN] brukes i [TiltakTilsynBarn]
 */
sealed interface TypeFaktaOgVurdering {
    val vilkårperiodeType: VilkårperiodeType
}

sealed interface TypeMålgruppeOgVurdering : TypeFaktaOgVurdering {
    override val vilkårperiodeType: MålgruppeType
}

sealed interface TypeAktivitetOgVurdering : TypeFaktaOgVurdering {
    override val vilkårperiodeType: AktivitetType
}

/**
 * Fakta, som kan inneholde ulike fakta, eks aktivitetsdager
 */
sealed interface Fakta
data object IngenFakta : Fakta

/**
 * Vurderinger, som kan inneholde ulike vurderinger, eks lønnet
 */
sealed interface Vurderinger {
    fun utledDelresultater(): List<ResultatDelvilkårperiode>
}

data object IngenVurderinger : Vurderinger {
    override fun utledDelresultater() = emptyList<ResultatDelvilkårperiode>()
}
