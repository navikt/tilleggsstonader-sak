package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import java.time.LocalDate

data class EndreAvklartDagRequest(
    val dato: LocalDate,
    val godkjentGjennomførtKjøring: Boolean,
    val parkeringsutgift: Int?,
    val begrunnelse: String? = null,
)
