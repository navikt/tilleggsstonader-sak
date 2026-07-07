package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

data class BeregningsresultatOffentligTransport(
    val reiser: List<BeregningsresultatOffentligTransportForSamling>,
)

data class BeregningsresultatOffentligTransportForSamling(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val utgifterOffentligTransport: Int?,
)
