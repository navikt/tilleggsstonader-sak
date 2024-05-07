package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering.EvalueringVilkårperiode.tilVurdering

object EvalueringAktivitet {
    fun utledResultat(
        type: AktivitetType,
        delvilkår: DelvilkårAktivitetDto,
    ): ResultatEvaluering {
        return when (type) {
            AktivitetType.UTDANNING, AktivitetType.REELL_ARBEIDSSØKER -> ikkeVurdertLønnet(type, delvilkår)
            AktivitetType.TILTAK -> utledResultat(delvilkår)
            AktivitetType.INGEN_AKTIVITET -> ikkeOppfyltUtenVurdering()
        }
    }

    private fun utledResultat(delvilkår: DelvilkårAktivitetDto): ResultatEvaluering {
        val lønnet = delvilkår.lønnet
        val vurderingLønnet = lønnet.tilVurdering(utledResultatLønnet(lønnet?.svar))
        val resultat = utledResultatVilkårperiode(vurderingLønnet.resultat)

        return ResultatEvaluering(
            delvilkår = DelvilkårAktivitet(lønnet = vurderingLønnet),
            resultat = resultat,
        )
    }

    private fun utledResultatVilkårperiode(
        resultatLønnet: ResultatDelvilkårperiode,
    ): ResultatVilkårperiode {
        return when (resultatLønnet) {
            ResultatDelvilkårperiode.IKKE_VURDERT -> ResultatVilkårperiode.IKKE_VURDERT
            ResultatDelvilkårperiode.IKKE_OPPFYLT -> ResultatVilkårperiode.IKKE_OPPFYLT
            ResultatDelvilkårperiode.OPPFYLT -> ResultatVilkårperiode.OPPFYLT
            else -> error("Ugyldig resultat=$resultatLønnet for tiltak er lønnet")
        }
    }

    private fun ikkeVurdertLønnet(type: AktivitetType, delvilkår: DelvilkårAktivitetDto): ResultatEvaluering {
        val lønnet = delvilkår.lønnet
        feilHvis(lønnet?.svar != null) {
            "Ugyldig svar=${lønnet?.svar} for lønnet for $type"
        }
        return ResultatEvaluering(
            delvilkår = DelvilkårAktivitet(
                lønnet = lønnet.tilVurdering(ResultatDelvilkårperiode.IKKE_AKTUELT),
            ),
            resultat = ResultatVilkårperiode.OPPFYLT,
        )
    }

    private fun ikkeOppfyltUtenVurdering() = ResultatEvaluering(
        delvilkår = DelvilkårAktivitet(
            lønnet = DelvilkårVilkårperiode.Vurdering(
                svar = null,
                resultat = ResultatDelvilkårperiode.IKKE_AKTUELT
            ),
        ),
        resultat = ResultatVilkårperiode.IKKE_OPPFYLT
    )

    private fun utledResultatLønnet(svar: SvarJaNei?) = when (svar) {
        SvarJaNei.JA -> ResultatDelvilkårperiode.IKKE_OPPFYLT
        SvarJaNei.NEI -> ResultatDelvilkårperiode.OPPFYLT
        null -> ResultatDelvilkårperiode.IKKE_VURDERT
        SvarJaNei.JA_IMPLISITT -> error("Svar=$svar er ikke gyldig svar for lønnet")
    }
}
