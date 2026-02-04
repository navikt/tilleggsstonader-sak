package no.nav.tilleggsstonader.sak.ekstern.stønad.dto

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate

data class RammevedtakDto(
    val id: ReiseId,
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
