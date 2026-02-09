package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate

data class InnsendtKjøreliste(
    val reiseId: ReiseId,
    val reisedager: List<KjørelisteDag>,
)

data class KjørelisteDag(
    val dato: LocalDate,
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
)
