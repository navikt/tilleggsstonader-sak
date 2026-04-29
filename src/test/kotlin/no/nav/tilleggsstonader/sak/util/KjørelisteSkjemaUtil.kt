package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.alleDatoer
import no.nav.tilleggsstonader.kontrakter.søknad.DatoFelt
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.Reisedag
import no.nav.tilleggsstonader.kontrakter.søknad.UkeMedReisedager
import no.nav.tilleggsstonader.kontrakter.søknad.VerdiFelt
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag

object KjørelisteSkjemaUtil {
    fun kjørelisteSkjema(
        reiseId: String,
        periode: Datoperiode,
        dagerKjørt: List<KjørtDag>,
    ): KjørelisteSkjema {
        require(dagerKjørt.all { periode.inneholder(it.dato) }) {
            "dagerKjørt må være innenfor perioden"
        }

        val reisedager: List<Reisedag> =
            periode.alleDatoer().map { dato ->
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
                        reisedagerLabel = "Ukentlige reisedager: 3",
                        spørsmål = "Harru kjørt",
                        reisedager = reisedager,
                    ),
                ),
            dokumentasjon = listOf(),
        )
    }
}
