package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaKjøreSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object FlyttingVilkårValidering {
    fun validerOppgitteFakta(fakta: FlyttingVilkårFakta) {
        brukerfeilHvis(fakta.adresse?.isBlank() == true) {
            "Flytteadressen kan ikke være tom"
        }
        when (fakta) {
            is FaktaFlyttebyrå -> {
                val navn1 = fakta.tilbud1.navn?.trim()
                val navn2 = fakta.tilbud2.navn?.trim()
                brukerfeilHvis((navn1 != null && navn1.isEmpty()) || (navn2 != null && navn2.isEmpty())) {
                    "Firmanavn kan ikke være tomt"
                }
                brukerfeilHvis(
                    navn1 != null && navn2 != null && navn1.equals(navn2, ignoreCase = true),
                ) {
                    "Tilbudene må være fra forskjellige flyttebyråer"
                }
                brukerfeilHvis(fakta.tilbud1.pris != null && fakta.tilbud1.pris <= 0) {
                    "Prisen på tilbudet må være større enn 0"
                }
                brukerfeilHvis(fakta.tilbud2.pris != null && fakta.tilbud2.pris <= 0) {
                    "Prisen på tilbudet må være større enn 0"
                }
            }

            is FaktaKjøreSelv -> {
                brukerfeilHvis(fakta.avstandEnVei != null && fakta.avstandEnVei <= 0) {
                    "Avstanden må være større enn 0"
                }
                listOf(fakta.henger, fakta.bompenger, fakta.ferge, fakta.parkering)
                    .filterNotNull()
                    .forEach { beløp ->
                        brukerfeilHvis(beløp < 0) { "Kostnader kan ikke være negative" }
                    }
            }

            else -> Unit
        }
    }

    fun erFullstendig(vilkår: LagreVilkårFlytting): Boolean {
        if (vilkår.fakta.adresse.isNullOrBlank()) return false

        val flyttebyråSvar = vilkår.svar[RegelId.SKAL_BRUKE_FLYTTEBYRÅ]?.svar ?: return false
        if (flyttebyråSvar == SvarId.JA) {
            val fakta = vilkår.fakta as? FaktaFlyttebyrå ?: return false
            val harNavnPåBeggeTilbud =
                !fakta.tilbud1.navn.isNullOrBlank() && !fakta.tilbud2.navn.isNullOrBlank()
            return harNavnPåBeggeTilbud && fakta.tilbud1.pris != null && fakta.tilbud2.pris != null
        }

        val kjøreSelvSvar = vilkår.svar[RegelId.SKAL_KJØRE_SELV]?.svar ?: return false
        return when (kjøreSelvSvar) {
            SvarId.NEI -> true
            SvarId.JA -> (vilkår.fakta as? FaktaKjøreSelv)?.avstandEnVei != null
            else -> false
        }
    }
}
