package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårFaktaReiseTilSamling
import java.math.BigDecimal

data class FaktaReiseTilSamling(
    val reiseId: ReiseId,
    val adresse: String?,
    val utgifterOffentligTransport: Int?,
    val reiseavstand: BigDecimal?,
) {
    fun mapTilVilkårFakta() =
        VilkårFaktaReiseTilSamling(
            reiseId = reiseId,
            adresse = adresse,
            utgifterOffentligTransport = utgifterOffentligTransport,
            reiseavstand = reiseavstand,
        )

//    private fun validerIngenNegativeUtgifter() {
//        utgifterOffentligTransport?.let {
//            brukerfeilHvis(it <= 0) {
//                "Utgifter til offentlig transport kan ikke være negative"
//            }
//        }
//    }
//
//    private fun validerReiseavstand() {
//        reiseavstand?.let {
//            brukerfeilHvis(it <= BigDecimal.ZERO) {
//                "Reiseavstand må være større enn 0"
//            }
//        }
//    }
}
