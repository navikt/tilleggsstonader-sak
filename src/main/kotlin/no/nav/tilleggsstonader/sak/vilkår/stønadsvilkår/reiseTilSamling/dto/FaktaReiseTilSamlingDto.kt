package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import java.math.BigDecimal

data class FaktaReiseTilSamlingDto(
    val utgifterOffentligTransport: Int?,
    val reiseavstand: BigDecimal?,
) {
    fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String,
    ) = FaktaReiseTilSamling(
        reiseId = reiseId,
        adresse = adresse,
        utgifterOffentligTransport = utgifterOffentligTransport,
        reiseavstand = reiseavstand,
    )
}
