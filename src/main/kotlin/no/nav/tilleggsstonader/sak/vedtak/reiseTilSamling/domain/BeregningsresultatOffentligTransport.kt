package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

data class BeregningsresultatOffentligTransport(
    val samling: List<BeregningsresultatForSamling>,
    val beløp: Int,
)

data class BeregningsresultatForSamling(
    val reiseId: ReiseId,
    val adresse: String?, // TODO trengs denne i domenet
    val fom: LocalDate,
    val tom: LocalDate,
    val utgifterOffentligTransport: Int?,
)
