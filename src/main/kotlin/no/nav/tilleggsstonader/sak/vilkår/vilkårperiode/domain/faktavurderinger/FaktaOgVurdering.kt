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
        return this.vurderinger.resultatVurderinger()
    }
}

sealed interface MålgruppeFaktaOgVurdering : FaktaOgVurdering {
    override val type: TypeMålgruppeOgVurdering
}

sealed interface AktivitetFaktaOgVurdering : FaktaOgVurdering {
    override val type: TypeAktivitetOgVurdering
}

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
    fun resultatVurderinger(): ResultatVilkårperiode {
        val resultater = finnVurderinger().map { it.resultat }
        return utledResultat(resultater)
    }

    fun inneholderGammelManglerData(): Boolean {
        val svar = finnVurderinger().map { it.svar }
        return svar.contains(SvarJaNei.GAMMEL_MANGLER_DATA)
    }

    private fun finnVurderinger(): MutableList<Vurdering> {
        val vurderinger = mutableListOf<Vurdering>()
        if (this is LønnetVurdering) {
            vurderinger.add(lønnet)
        }
        if (this is MedlemskapVurdering) {
            vurderinger.add(medlemskap)
        }
        if (this is DekketAvAnnetRegelverkVurdering) {
            vurderinger.add(dekketAvAnnetRegelverk)
        }
        if (this is HarUtgifterVurdering) {
            vurderinger.add(harUtgifter)
        }
        if (this is HarRettTilUtstyrsstipendVurdering) {
            vurderinger.add(harRettTilUtstyrsstipend)
        }
        if (this is AldersvilkårVurdering) {
            vurderinger.add(aldersvilkår)
        }
        if (this is MottarSykepengerForFulltidsstillingVurdering) {
            vurderinger.add(mottarSykepengerForFulltidsstilling)
        }
        return vurderinger
    }

    private fun utledResultat(resultater: List<ResultatDelvilkårperiode>) =
        when {
            resultater.contains(ResultatDelvilkårperiode.IKKE_VURDERT) -> ResultatVilkårperiode.IKKE_VURDERT
            resultater.contains(ResultatDelvilkårperiode.IKKE_OPPFYLT) -> ResultatVilkårperiode.IKKE_OPPFYLT
            resultater.all { it == ResultatDelvilkårperiode.OPPFYLT } -> ResultatVilkårperiode.OPPFYLT
            else -> error("Ugyldig resultat ($resultater)")
        }
}

data object IngenVurderinger : Vurderinger
