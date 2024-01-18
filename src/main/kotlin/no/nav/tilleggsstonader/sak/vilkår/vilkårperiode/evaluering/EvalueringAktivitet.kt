package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.tilVurdering

object EvalueringAktivitet {
    fun utledResultat(
        type: AktivitetType,
        delvilkår: DelvilkårAktivitetDto,
    ): ResultatEvaluering {
        val oppdatertDelvilkår = utledVurderingerDelvilkår(type, delvilkår)
        val resultatAktivitet = utledResultatVilkårperiode(oppdatertDelvilkår)
        return ResultatEvaluering(oppdatertDelvilkår, resultatAktivitet)
    }

    private fun utledVurderingerDelvilkår(
        type: AktivitetType,
        delvilkår: DelvilkårAktivitetDto,
    ): DelvilkårAktivitet {
        val vurderingLønnet = vurderingLønnet(type, delvilkår.lønnet)
        val vurderingMottarSykepenger = vurderingMottarSykepenger(delvilkår.mottarSykepenger)

        return DelvilkårAktivitet(
            lønnet = vurderingLønnet,
            mottarSykepenger = vurderingMottarSykepenger,
        )
    }

    private fun utledResultatVilkårperiode(
        delvilkår: DelvilkårAktivitet,
    ): ResultatVilkårperiode {
        val vurderingLønnet = delvilkår.lønnet
        val vurderingMottarSykepenger = delvilkår.mottarSykepenger
        val resultater = listOf(
            vurderingLønnet.resultat,
            vurderingMottarSykepenger.resultat,
        ).filterNot { it == ResultatDelvilkårperiode.IKKE_AKTUELT }

        return when {
            resultater.contains(ResultatDelvilkårperiode.IKKE_VURDERT) -> ResultatVilkårperiode.IKKE_VURDERT
            resultater.contains(ResultatDelvilkårperiode.IKKE_OPPFYLT) -> ResultatVilkårperiode.IKKE_OPPFYLT
            resultater.all { it == ResultatDelvilkårperiode.OPPFYLT } -> ResultatVilkårperiode.OPPFYLT
            else ->
                error(
                    "Ugyldig resultat resultatLønnet=$vurderingLønnet" +
                        " resultatMottarSykepenger=$vurderingMottarSykepenger",
                )
        }
    }

    private fun vurderingLønnet(type: AktivitetType, vurderingDto: VurderingDto?): Vurdering {
        return when (type) {
            AktivitetType.TILTAK -> vurderingDto.tilVurdering(utledResultatLønnet(vurderingDto?.svar))

            AktivitetType.UTDANNING,
            AktivitetType.REELL_ARBEIDSSØKER,
            -> ikkeVurdertLønnet(type, vurderingDto)
        }
    }

    private fun ikkeVurdertLønnet(type: AktivitetType, vurderingDto: VurderingDto?): Vurdering {
        val svar = vurderingDto?.svar
        feilHvis(svar != null) {
            "Ugyldig svar=$svar for lønnet for $type"
        }
        return vurderingDto.tilVurdering(ResultatDelvilkårperiode.IKKE_AKTUELT)
    }

    private fun utledResultatLønnet(svar: SvarJaNei?) = when (svar) {
        SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
        SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
        null -> ResultatDelvilkårperiode.IKKE_VURDERT
        SvarJaNei.JA_IMPLISITT -> error("Svar=$svar er ikke gyldig svar for lønnet")
    }

    private fun vurderingMottarSykepenger(vurderingDto: VurderingDto?): Vurdering =
        vurderingDto.tilVurdering(utledResultatMottarSykepenger(vurderingDto?.svar))

    private fun utledResultatMottarSykepenger(svar: SvarJaNei?) = when (svar) {
        SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
        SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
        null -> ResultatDelvilkårperiode.IKKE_VURDERT
        SvarJaNei.JA_IMPLISITT -> error("Svar=$svar er ikke gyldig svar for mottarSykepenger")
    }
}
