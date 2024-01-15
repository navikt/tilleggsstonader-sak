package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerAktivitet
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.DetaljerVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode.IKKE_OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode.IKKE_TATT_STILLING_TIL
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode.OPPFYLT
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
            -> OPPFYLT

            SvarJaNei.NEI -> IKKE_OPPFYLT
            SvarJaNei.IKKE_VURDERT -> IKKE_TATT_STILLING_TIL
        }
    }

    private fun utledResultatAktivitet(detaljer: DetaljerAktivitet): ResultatVilkårperiode {
        val resultatLønnet = utledResultatLønnet(detaljer)
        val resultatMottarSykepenger = utledResultatMottarSykepenger(detaljer)

        val resultater = listOf(resultatLønnet, resultatMottarSykepenger)

        return when {
            resultater.contains(IKKE_TATT_STILLING_TIL) -> IKKE_TATT_STILLING_TIL
            resultater.contains(IKKE_OPPFYLT) -> IKKE_OPPFYLT
            resultatLønnet == OPPFYLT && resultatMottarSykepenger == OPPFYLT -> OPPFYLT
            else -> error("Ugyldig resultat resultatLønnet=$resultatLønnet resultatMottarSykepenger=$resultatMottarSykepenger")
        }
    }

    private fun utledResultatLønnet(detaljer: DetaljerAktivitet) =
        when (detaljer.lønnet) {
            SvarJaNei.JA -> IKKE_OPPFYLT
            SvarJaNei.NEI -> OPPFYLT
            SvarJaNei.IKKE_VURDERT -> IKKE_TATT_STILLING_TIL
            SvarJaNei.JA_IMPLISITT -> error("Ikke gyldig svar for lønnet")
        }

    private fun utledResultatMottarSykepenger(detaljer: DetaljerAktivitet) =
        when (detaljer.mottarSykepenger) {
            SvarJaNei.JA -> IKKE_OPPFYLT
            SvarJaNei.NEI -> OPPFYLT
            SvarJaNei.IKKE_VURDERT -> IKKE_TATT_STILLING_TIL
            SvarJaNei.JA_IMPLISITT -> error("Ikke gyldig svar for mottarSykepenger")
        }
}
