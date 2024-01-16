package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode.IKKE_OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode.IKKE_VURDERT
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode.OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårVilkårperiodeDto

object EvalueringVilkårperiode {

    data class ResultatEvaluering(
        val delvilkår: DelvilkårVilkårperiode,
        val resultat: ResultatVilkårperiode,
    )

    fun evaulerVilkårperiode(delvilkår: DelvilkårVilkårperiodeDto): ResultatEvaluering {
        return when (delvilkår) {
            is DelvilkårMålgruppeDto -> delvilkår.utledResultat()
            is DelvilkårAktivitetDto -> delvilkår.utledResultat()
        }
    }

    fun DelvilkårMålgruppeDto.utledResultat(): ResultatEvaluering {
        val resultatDelvilkår = utledResultatMedlemskap(medlemskap)
        val oppdatertDelvilkår = DelvilkårMålgruppe(medlemskap = Vurdering(svar = medlemskap, resultatDelvilkår))
        val resultatVilkår = when (resultatDelvilkår) {
            ResultatDelvilkårperiode.OPPFYLT -> ResultatVilkårperiode.OPPFYLT
            ResultatDelvilkårperiode.IKKE_OPPFYLT -> ResultatVilkårperiode.IKKE_OPPFYLT
            ResultatDelvilkårperiode.IKKE_VURDERT -> ResultatVilkårperiode.IKKE_VURDERT
        }
        return ResultatEvaluering(oppdatertDelvilkår, resultatVilkår)
    }

    private fun utledResultatMedlemskap(svar: SvarJaNei?): ResultatDelvilkårperiode = when (svar) {
        SvarJaNei.JA,
        SvarJaNei.JA_IMPLISITT,
        -> ResultatDelvilkårperiode.OPPFYLT

        SvarJaNei.NEI -> ResultatDelvilkårperiode.IKKE_OPPFYLT
        null -> ResultatDelvilkårperiode.IKKE_VURDERT
    }

    fun DelvilkårAktivitetDto.utledResultat(): ResultatEvaluering {
        val resultatLønnet = utledResultatLønnet(lønnet)
        val resultatMottarSykepenger = utledResultatMottarSykepenger(mottarSykepenger)

        val resultater = listOf(resultatLønnet, resultatMottarSykepenger)

        val resultatAktivitet = when {
            resultater.contains(ResultatDelvilkårperiode.IKKE_VURDERT) -> IKKE_VURDERT
            resultater.contains(ResultatDelvilkårperiode.IKKE_OPPFYLT) -> IKKE_OPPFYLT
            resultater.all { it == ResultatDelvilkårperiode.OPPFYLT } -> OPPFYLT
            resultatLønnet == ResultatDelvilkårperiode.OPPFYLT && resultatMottarSykepenger == ResultatDelvilkårperiode.OPPFYLT -> OPPFYLT
            else -> error("Ugyldig resultat resultatLønnet=$resultatLønnet resultatMottarSykepenger=$resultatMottarSykepenger")
        }
        val oppdatertDelvilkår = DelvilkårAktivitet(
            lønnet = Vurdering(lønnet, resultatLønnet),
            mottarSykepenger = Vurdering(mottarSykepenger, resultatMottarSykepenger),
        )
        return ResultatEvaluering(oppdatertDelvilkår, resultatAktivitet)
    }

    private fun utledResultatLønnet(svar: SvarJaNei?) =
        when (svar) {
            SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
            SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
            null -> ResultatDelvilkårperiode.IKKE_VURDERT
            SvarJaNei.JA_IMPLISITT -> error("Ikke gyldig svar for lønnet")
        }

    private fun utledResultatMottarSykepenger(svar: SvarJaNei?) =
        when (svar) {
            SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
            SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
            null -> ResultatDelvilkårperiode.IKKE_VURDERT
            SvarJaNei.JA_IMPLISITT -> error("Ikke gyldig svar for mottarSykepenger")
        }
}
