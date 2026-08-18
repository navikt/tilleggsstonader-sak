package no.nav.tilleggsstonader.sak.ekstern.stønad

import java.time.LocalDate

data class ManueltRegistrertKjørelisteEksternDto(
    val reisedager: List<ManueltRegistrertKjørelisteDagEksternDto>,
)

data class ManueltRegistrertKjørelisteDagEksternDto(
    val dato: LocalDate,
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)
