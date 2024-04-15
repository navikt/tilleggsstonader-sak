package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.tilVurdering

object EvalueringMålgruppe {

    fun utledResultat(type: MålgruppeType, delvilkår: DelvilkårMålgruppeDto): ResultatEvaluering {
        val oppdatertDelvilkår = utledVurderingerDelvilkår(type, delvilkår)
        val resultatMålgruppe = utledResultatVilkårperiode(oppdatertDelvilkår)

        return ResultatEvaluering(delvilkår = oppdatertDelvilkår, resultat = resultatMålgruppe)
    }

    private fun utledVurderingerDelvilkår(type: MålgruppeType, delvilkår: DelvilkårMålgruppeDto): DelvilkårMålgruppe {
        val vurderingMedlemskap = vurderingMedlemskap(type, delvilkår.medlemskap)
        val vurderingDekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk(type, delvilkår.dekketAvAnnetRegelverk)

        return DelvilkårMålgruppe(
            medlemskap = vurderingMedlemskap,
            dekketAvAnnetRegelverk = vurderingDekketAvAnnetRegelverk,
        )
    }

    private fun utledResultatVilkårperiode(
        delvilkår: DelvilkårMålgruppe,
    ): ResultatVilkårperiode {
        val vurderingMedlemskap = delvilkår.medlemskap
        val vurderingDekketAvAnnetRegelverk = delvilkår.dekketAvAnnetRegelverk

        val resultater = listOf(
            vurderingMedlemskap.resultat,
            vurderingDekketAvAnnetRegelverk.resultat,
        ).filterNot { it == ResultatDelvilkårperiode.IKKE_AKTUELT }

        return when {
            resultater.contains(ResultatDelvilkårperiode.IKKE_VURDERT) -> ResultatVilkårperiode.IKKE_VURDERT
            resultater.contains(ResultatDelvilkårperiode.IKKE_OPPFYLT) -> ResultatVilkårperiode.IKKE_OPPFYLT
            resultater.all { it == ResultatDelvilkårperiode.OPPFYLT } -> ResultatVilkårperiode.OPPFYLT
            else ->
                error(
                    "Ugyldig resultat vurderingMedlemskap=$vurderingMedlemskap" +
                        " vurderingDekketAvAnnetRegelverk=$vurderingDekketAvAnnetRegelverk",
                )
        }
    }

    private fun vurderingMedlemskap(
        type: MålgruppeType,
        vurdering: VurderingDto?,
    ): Vurdering {
        return when (type) {
            MålgruppeType.AAP,
            MålgruppeType.OVERGANGSSTØNAD,
            -> jaImplisitt(vurdering, type)

            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.OMSTILLINGSSTØNAD,
            MålgruppeType.UFØRETRYGD,
            -> vurdering.tilVurdering(utledResultatMedlemskap(vurdering?.svar))

            MålgruppeType.DAGPENGER -> vurdering.tilVurdering(utledResultatMedlemskap(vurdering?.svar)) // Trenger denne egt å være egen?
        }
    }

    private fun vurderingDekketAvAnnetRegelverk(
        type: MålgruppeType,
        vurdering: VurderingDto?,
    ): Vurdering {
        return when (type) {
            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.AAP,
            MålgruppeType.UFØRETRYGD,
            -> vurdering.tilVurdering(utledResultatDekketAvAnnetRegelverk(vurdering?.svar, type))

            else -> ikkeVurdertDekketAvAnnetRegelverk(type, vurdering)
        }
    }

    private fun utledResultatDekketAvAnnetRegelverk(svar: SvarJaNei?, type: MålgruppeType) = when (svar) {
        SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
        SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
        null -> ResultatDelvilkårperiode.IKKE_VURDERT
        SvarJaNei.JA_IMPLISITT -> error(ugyldigSvarDekketAvAnnetRegelverkFeilmelding(svar, type))
    }

    private fun ikkeVurdertDekketAvAnnetRegelverk(type: MålgruppeType, vurderingDto: VurderingDto?): Vurdering {
        val svar = vurderingDto?.svar
        feilHvis(svar != null) {
            ugyldigSvarDekketAvAnnetRegelverkFeilmelding(svar, type)
        }
        return vurderingDto.tilVurdering(ResultatDelvilkårperiode.IKKE_AKTUELT)
    }

    private fun jaImplisitt(
        vurdering: VurderingDto?,
        type: MålgruppeType,
    ): Vurdering {
        val svar = vurdering?.svar
        feilHvis(svar != null && svar != SvarJaNei.JA_IMPLISITT) {
            "Kan ikke evaluere svar=$svar på medlemskap for type=$type"
        }
        return IMPLISITT_OPPFYLT_MÅLGRUPPE
    }

    private fun utledResultatMedlemskap(svar: SvarJaNei?): ResultatDelvilkårperiode = when (svar) {
        SvarJaNei.JA -> ResultatDelvilkårperiode.OPPFYLT
        SvarJaNei.JA_IMPLISITT -> error("Ugyldig svar=$svar")
        SvarJaNei.NEI -> ResultatDelvilkårperiode.IKKE_OPPFYLT
        null -> ResultatDelvilkårperiode.IKKE_VURDERT
    }

    val IMPLISITT_OPPFYLT_MÅLGRUPPE =
        Vurdering(
            svar = SvarJaNei.JA_IMPLISITT,
            begrunnelse = null,
            resultat = ResultatDelvilkårperiode.OPPFYLT,
        )

    private fun ugyldigSvarDekketAvAnnetRegelverkFeilmelding(svar: SvarJaNei?, type: MålgruppeType) =
        "Ugyldig svar=$svar for dekket av annet regelverk for $type"
}
