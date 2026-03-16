package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object KjørelisteUtil {
    fun kjøreliste(
        id: UUID = UUID.randomUUID(),
        journalpostId: String = "12345678",
        fagsakId: FagsakId = FagsakId.random(),
        datoMottatt: LocalDateTime = LocalDateTime.now(),
        reiseId: ReiseId = ReiseId.random(),
        periode: Datoperiode,
        kjørteDager: List<KjørtDag> =
            listOf(
                KjørtDag(dato = LocalDate.now(), parkeringsutgift = null),
            ),
    ): Kjøreliste {
        val reisedager: List<KjørelisteDag> =
            periode.alleDatoer().map { dato ->
                val kjørtDag = kjørteDager.singleOrNull { it.dato == dato }
                KjørelisteDag(
                    dato = dato,
                    harKjørt = kjørtDag != null,
                    parkeringsutgift = kjørtDag?.parkeringsutgift,
                )
            }

        return Kjøreliste(
            id = id,
            journalpostId = journalpostId,
            fagsakId = fagsakId,
            datoMottatt = datoMottatt,
            data =
                InnsendtKjøreliste(
                    reiseId = reiseId,
                    reisedager = reisedager,
                ),
        )
    }

    data class KjørtDag(
        val dato: LocalDate,
        val parkeringsutgift: Int? = null,
    )
}
