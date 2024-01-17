package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto

object EvalueringMålgruppe {

    fun utledResultat(
        type: MålgruppeType,
        delvilkår: DelvilkårMålgruppeDto,
    ): ResultatEvaluering {
        return when (type) {
            MålgruppeType.AAP,
            MålgruppeType.OVERGANGSSTØNAD,
            MålgruppeType.UFØRETRYGD,
            -> jaImplicitt(delvilkår, type)

            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.OMSTILLINGSSTØNAD,
            -> utledResultat(delvilkår)
        }
    }

    private fun jaImplicitt(
        delvilkår: DelvilkårMålgruppeDto,
        type: MålgruppeType,
    ): ResultatEvaluering {
        val medlemskap = delvilkår.medlemskap
        feilHvis(medlemskap != null && medlemskap != SvarJaNei.JA_IMPLISITT) {
            "Kan ikke evaluere svar=$medlemskap på medlemskap for type=$type"
        }
        return IMPLICITT_OPPFYLT_MÅLGRUPPE
    }

    private fun utledResultat(delvilkår: DelvilkårMålgruppeDto): ResultatEvaluering {
        val medlemskap = delvilkår.medlemskap
        val resultatDelvilkår = utledResultatMedlemskap(medlemskap)
        val oppdatertDelvilkår = DelvilkårMålgruppe(medlemskap = Vurdering(svar = medlemskap, resultatDelvilkår))
        val resultatVilkår = when (resultatDelvilkår) {
            ResultatDelvilkårperiode.OPPFYLT -> ResultatVilkårperiode.OPPFYLT
            ResultatDelvilkårperiode.IKKE_OPPFYLT -> ResultatVilkårperiode.IKKE_OPPFYLT
            ResultatDelvilkårperiode.IKKE_VURDERT -> ResultatVilkårperiode.IKKE_VURDERT
            ResultatDelvilkårperiode.IKKE_AKTUELT -> error("Ikke gyldig resultat for målgruppe $resultatDelvilkår")
        }
        return ResultatEvaluering(oppdatertDelvilkår, resultatVilkår)
    }

    private fun utledResultatMedlemskap(svar: SvarJaNei?): ResultatDelvilkårperiode = when (svar) {
        SvarJaNei.JA -> ResultatDelvilkårperiode.OPPFYLT
        SvarJaNei.JA_IMPLISITT -> error("Ugyldig svar=$svar")
        SvarJaNei.NEI -> ResultatDelvilkårperiode.IKKE_OPPFYLT
        null -> ResultatDelvilkårperiode.IKKE_VURDERT
    }

    val IMPLICITT_OPPFYLT_MÅLGRUPPE = ResultatEvaluering(
        DelvilkårMålgruppe(
            medlemskap = Vurdering(
                svar = SvarJaNei.JA_IMPLISITT,
                resultat = ResultatDelvilkårperiode.OPPFYLT,
            ),
        ),
        resultat = ResultatVilkårperiode.OPPFYLT,
    )
}
