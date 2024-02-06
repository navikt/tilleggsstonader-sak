package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.tilVurdering

object EvalueringMålgruppe {

    fun utledResultat(
        type: MålgruppeType,
        delvilkår: DelvilkårMålgruppeDto,
    ): ResultatEvaluering {
        return when (type) {
            MålgruppeType.AAP,
            MålgruppeType.OVERGANGSSTØNAD,
            -> jaImplisitt(delvilkår, type)

            MålgruppeType.NEDSATT_ARBEIDSEVNE,
            MålgruppeType.OMSTILLINGSSTØNAD,
            MålgruppeType.UFØRETRYGD,
            -> utledResultat(delvilkår)

            MålgruppeType.DAGPENGER -> utledResultat(delvilkår) // Foreløpig
        }
    }

    private fun jaImplisitt(
        delvilkår: DelvilkårMålgruppeDto,
        type: MålgruppeType,
    ): ResultatEvaluering {
        val medlemskap = delvilkår.medlemskap?.svar
        feilHvis(medlemskap != null && medlemskap != SvarJaNei.JA_IMPLISITT) {
            "Kan ikke evaluere svar=$medlemskap på medlemskap for type=$type"
        }
        return IMPLISITT_OPPFYLT_MÅLGRUPPE
    }

    private fun utledResultat(delvilkår: DelvilkårMålgruppeDto): ResultatEvaluering {
        val medlemskap = delvilkår.medlemskap
        val resultatDelvilkår = utledResultatMedlemskap(medlemskap?.svar)
        val oppdatertDelvilkår = DelvilkårMålgruppe(medlemskap = medlemskap.tilVurdering(resultatDelvilkår))
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

    val IMPLISITT_OPPFYLT_MÅLGRUPPE = ResultatEvaluering(
        DelvilkårMålgruppe(
            medlemskap = Vurdering(
                svar = SvarJaNei.JA_IMPLISITT,
                begrunnelse = null,
                resultat = ResultatDelvilkårperiode.OPPFYLT,
            ),
        ),
        resultat = ResultatVilkårperiode.OPPFYLT,
    )
}
