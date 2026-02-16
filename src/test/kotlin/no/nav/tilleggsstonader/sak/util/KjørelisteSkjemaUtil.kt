package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.Reisedag
import no.nav.tilleggsstonader.kontrakter.søknad.UkeMedReisedager
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object KjørelisteSkjemaUtil {
    fun kjørelisteSkjema(
        reiseId: String,
        periode: Datoperiode,
        dagerKjørt: List<KjørtDag>,
    ): KjørelisteSkjema {
        require(dagerKjørt.all { periode.inneholder(it.dato) }) {
            "dagerKjørt må være innenfor perioden"
        }

        val alleDager: List<LocalDate> =
            (0L..ChronoUnit.DAYS.between(periode.fom, periode.tom))
                .map { periode.fom.plusDays(it) }

        val reisedager: List<Reisedag> =
            alleDager.map { dato ->
                Reisedag(
                    dato = DatoFelt(label = "Dato", verdi = dato),
                    harKjørt = dagerKjørt.any { it.dato == dato },
                    parkeringsutgift = VerdiFelt(dagerKjørt.singleOrNull { it.dato == dato }?.parkeringsutgift, "Kroner"),
                )
            }

        return KjørelisteSkjema(
            reiseId = reiseId,
            reisedagerPerUkeAvsnitt =
                listOf(
                    UkeMedReisedager(
                        ukeLabel = "Uke 1",
                        spørsmål = "Harru kjørt",
                        reisedager = reisedager,
                    ),
                ),
            dokumentasjon = listOf(),
        )
    }

    data class KjørtDag(
        val dato: LocalDate,
        val parkeringsutgift: Int? = null,
    )
}
