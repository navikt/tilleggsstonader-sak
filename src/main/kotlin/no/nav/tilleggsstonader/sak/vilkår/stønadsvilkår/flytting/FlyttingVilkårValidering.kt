package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.FlyttingRegel
import java.math.BigDecimal

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
                if (fakta.nødvendigUtgift.erUtgiftenNødvendig.ja()) {
                    brukerfeilHvis(fakta.beløpSomKanDekkes != null) {
                        "Beløp som kan dekkes skal ikke være satt når hele utgiften er nødvendig"
                    }
                } else {
                    brukerfeilHvis(fakta.beløpSomKanDekkes != null && fakta.beløpSomKanDekkes <= BigDecimal.ZERO) {
                        "Beløp som kan dekkes må være større enn 0"
                    }
                }
            }

            is FaktaFlytteSelv -> {
                brukerfeilHvis(fakta.avstandEnVei != null && fakta.avstandEnVei <= 0) {
                    "Avstanden må være større enn 0"
                }
                listOfNotNull(fakta.henger, fakta.leiebil, fakta.bompenger, fakta.ferge, fakta.parkering, fakta.annet)
                    .forEach { beløp ->
                        brukerfeilHvis(beløp < BigDecimal.ZERO) { "Kostnader kan ikke være negative" }
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
                        fakta.tilbud.all { !it.navn.isNullOrBlank() && it.pris != null } &&
                        (fakta.nødvendigUtgift.erUtgiftenNødvendig.ja() || fakta.beløpSomKanDekkes != null)
                }

                SvarId.FLYTTER_SELV -> (vilkår.fakta as? FaktaFlytteSelv)?.avstandEnVei != null
                else -> false
            }
        }

        return true
    }
}
