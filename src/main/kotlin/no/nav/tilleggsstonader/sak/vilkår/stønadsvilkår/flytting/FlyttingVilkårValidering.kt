package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingRegel

object FlyttingVilkårValidering {
    private val flyttingRegel = FlyttingRegel()

    fun validerOppgitteFakta(fakta: FlyttingVilkårFakta) {
        brukerfeilHvis(fakta.adresse.isBlank()) { "Flytteadressen kan ikke være tom" }
        when (fakta) {
            is FaktaFlyttebyrå -> {
                brukerfeilHvis(fakta.tilbud.size != 2) {
                    "Det må være nøyaktig to tilbud fra flyttebyrå"
                }
                val navn = fakta.tilbud.mapNotNull { it.navn?.trim() }
                brukerfeilHvis(navn.any { it.isEmpty() }) {
                    "Firmanavn kan ikke være tomt"
                }
                brukerfeilHvis(navn.distinctBy { it.lowercase() }.size != navn.size) {
                    "Tilbudene må være fra forskjellige flyttebyråer"
                }
                brukerfeilHvis(fakta.tilbud.any { it.pris != null && it.pris <= 0 }) {
                    "Prisen på tilbudet må være større enn 0"
                }
            }

            is FaktaFlytteSelv -> {
                brukerfeilHvis(fakta.avstandEnVei != null && fakta.avstandEnVei <= 0) {
                    "Avstanden må være større enn 0"
                }
                listOfNotNull(fakta.henger, fakta.bompenger, fakta.ferge, fakta.parkering)
                    .forEach { beløp ->
                        brukerfeilHvis(beløp < 0) { "Kostnader kan ikke være negative" }
                    }
            }

            else -> Unit
        }
    }

    fun erFullstendig(vilkår: LagreVilkårFlytting): Boolean {
        if (vilkår.fakta.adresse.isBlank()) return false

        val flyttemåte = vilkår.svar[RegelId.HVORDAN_SKAL_BRUKER_FLYTTE]?.svar

        if (flyttemåte != null) {
            return when (flyttemåte) {
                SvarId.FLYTTEBYRÅ -> {
                    val fakta = vilkår.fakta as? FaktaFlyttebyrå ?: return false
                    fakta.tilbud.size == 2 &&
                        fakta.tilbud.all { !it.navn.isNullOrBlank() && it.pris != null }
                }

                SvarId.FLYTTER_SELV -> (vilkår.fakta as? FaktaFlytteSelv)?.avstandEnVei != null
                else -> false
            }
        }

        return true
    }
}
