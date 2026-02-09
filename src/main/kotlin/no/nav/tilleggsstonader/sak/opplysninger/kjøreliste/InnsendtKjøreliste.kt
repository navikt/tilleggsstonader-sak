package no.nav.tilleggsstonader.sak.opplysninger.kjøreliste

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate

data class InnsendtKjøreliste(
    val reiseId: ReiseId,
    val dagerKjørt: List<KjørelisteDag>,
)

data class KjørelisteDag(
    val dato: LocalDate,
    val parkeringsutgift: Int?,
)
