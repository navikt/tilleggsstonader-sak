package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.Reisedag
import no.nav.tilleggsstonader.kontrakter.søknad.UkeMedReisedager
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import java.time.LocalDate

object KjørelisteSkjemaUtil {
    fun kjørelisteSkjema() =
        KjørelisteSkjema(
            reisedagerPerUkeAvsnitt =
                listOf(
                    UkeMedReisedager(
                        ukeLabel = "Uke 1",
                        spørsmål = "Harru kjørt",
                        reisedager =
                            listOf(
                                Reisedag(
                                    dato =
                                        DatoFelt(
                                            label = "I dag",
                                            verdi = LocalDate.now(),
                                        ),
                                    harKjørt = true,
                                    parkeringsutgift = VerdiFelt(90, "Kroner"),
                                ),
                            ),
                    ),
                ),
            dokumentasjon = listOf(),
        )
}
