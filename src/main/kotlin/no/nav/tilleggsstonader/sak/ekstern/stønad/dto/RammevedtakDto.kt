package no.nav.tilleggsstonader.sak.ekstern.stønad.dto

import java.time.LocalDate

data class RammevedtakDto(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val reisedagerPerUke: Int,
    val aktivitetsadresse: String,
    val aktivitetsnavn: String,
    val uker: List<RammevedtakUkeDto>,
)

data class RammevedtakUkeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val ukeNummer: Int,
)
