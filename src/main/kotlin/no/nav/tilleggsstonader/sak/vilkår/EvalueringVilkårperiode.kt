package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerAktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.SvarJaNei

object EvalueringVilkårperiode {

    fun evaulerVilkårperiode(detaljer: DetaljerVilkårperiode): ResultatVilkårperiode {
        return when (detaljer) {
            is DetaljerMålgruppe -> utledResultatMålgruppe(detaljer)
            is DetaljerAktivitet -> utledResultatAktivitet(detaljer)
        }
    }

    private fun utledResultatMålgruppe(detaljer: DetaljerMålgruppe): ResultatVilkårperiode {
        return when (detaljer.medlemskap) {
            SvarJaNei.JA,
            SvarJaNei.JA_IMPLISITT,
            -> ResultatVilkårperiode.OPPFYLT

            SvarJaNei.NEI -> ResultatVilkårperiode.IKKE_OPPFYLT
            SvarJaNei.IKKE_VURDERT -> ResultatVilkårperiode.IKKE_TATT_STILLING_TIL
        }
    }

    private fun utledResultatAktivitet(detaljer: DetaljerAktivitet): ResultatVilkårperiode {
        val resultatLønnet = utledResultatLønnet(detaljer)
        val resultatMottarSykepenger = utledResultatMottarSykepenger(detaljer)

        return when {
            oneOf(
                resultatLønnet,
                resultatMottarSykepenger,
                ResultatVilkårperiode.IKKE_TATT_STILLING_TIL,
            ) -> ResultatVilkårperiode.IKKE_TATT_STILLING_TIL

            oneOf(
                resultatLønnet,
                resultatMottarSykepenger,
                ResultatVilkårperiode.IKKE_OPPFYLT,
            ) -> ResultatVilkårperiode.IKKE_OPPFYLT

            resultatLønnet == ResultatVilkårperiode.OPPFYLT && resultatMottarSykepenger == ResultatVilkårperiode.OPPFYLT -> ResultatVilkårperiode.OPPFYLT
            else -> error("Ugyldig resultat resultatLønnet=$resultatLønnet resultatMottarSykepenger=$resultatMottarSykepenger")
        }
    }

    private fun oneOf(
        resultatLønnet: ResultatVilkårperiode,
        resultatMottarSykepenger: ResultatVilkårperiode,
        testResultat: ResultatVilkårperiode,
    ): Boolean = resultatLønnet == testResultat || resultatMottarSykepenger == testResultat

    private fun utledResultatLønnet(detaljer: DetaljerAktivitet) =
        when (detaljer.lønnet) {
            SvarJaNei.JA -> ResultatVilkårperiode.IKKE_OPPFYLT
            SvarJaNei.NEI -> ResultatVilkårperiode.OPPFYLT
            SvarJaNei.IKKE_VURDERT -> ResultatVilkårperiode.IKKE_TATT_STILLING_TIL
            SvarJaNei.JA_IMPLISITT -> error("Ikke gyldig svar for lønnet")
        }

    private fun utledResultatMottarSykepenger(detaljer: DetaljerAktivitet) =
        when (detaljer.mottarSykepenger) {
            SvarJaNei.JA -> ResultatVilkårperiode.IKKE_OPPFYLT
            SvarJaNei.NEI -> ResultatVilkårperiode.OPPFYLT
            SvarJaNei.IKKE_VURDERT -> ResultatVilkårperiode.IKKE_TATT_STILLING_TIL
            SvarJaNei.JA_IMPLISITT -> error("Ikke gyldig svar for mottarSykepenger")
        }
}
