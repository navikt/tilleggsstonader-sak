package no.nav.tilleggsstonader.sak.privatbil.avklartedager

import java.time.LocalDate

data class EndreAvklartUkeRequest(
    val godtarAvvik: Boolean,
    val dager: List<EndreAvklartDagRequest>,
)

data class EndreAvklartDagRequest(
    val dato: LocalDate,
    val godkjentGjennomførtKjøring: Boolean,
    val parkeringsutgift: Int?,
    val begrunnelse: String?,
)
