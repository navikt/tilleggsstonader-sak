package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import java.time.LocalDate

data class UtgiftOffentligTransport(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallReisedagerPerUke: Int,
    val prisEnkelbillett: Int,
    val pris30dagersbillett: Int,
    val pris7dagersbillett: Int,
)
