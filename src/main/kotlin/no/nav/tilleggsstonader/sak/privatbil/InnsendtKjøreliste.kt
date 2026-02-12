package no.nav.tilleggsstonader.sak.privatbil

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate

data class InnsendtKjøreliste(
    val reiseId: ReiseId,
    val reisedager: List<KjørelisteDag>,
) : Periode<LocalDate> {
    @JsonIgnore
    override val fom: LocalDate = reisedager.minOf { it.dato }

    @JsonIgnore
    override val tom: LocalDate = reisedager.maxOf { it.dato }
}

data class KjørelisteDag(
    val dato: LocalDate,
    val harKjørt: Boolean,
    val parkeringsutgift: Int?,
) {
    init {
        if (!harKjørt) {
            require(parkeringsutgift == null)
        }
    }
}
