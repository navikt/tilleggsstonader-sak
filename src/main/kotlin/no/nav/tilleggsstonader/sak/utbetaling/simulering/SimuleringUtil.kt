package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Simuleringsoppsummering
import no.nav.tilleggsstonader.sak.util.Månedsperiode
import java.math.BigDecimal

fun Simuleringsoppsummering.hentSammenhengendePerioderMedFeilutbetaling(): List<Månedsperiode> {
    val perioderMedFeilutbetaling =
        perioder.sortedBy { it.fom }.filter { it.feilutbetaling > BigDecimal(0) }.map {
            Månedsperiode(it.fom, it.tom)
        }

    return perioderMedFeilutbetaling.fold(mutableListOf()) { akkumulatorListe, nestePeriode ->
        val gjeldendePeriode = akkumulatorListe.lastOrNull()

        if (gjeldendePeriode != null && erPerioderSammenhengende(gjeldendePeriode, nestePeriode)) {
            val oppdatertGjeldendePeriode = gjeldendePeriode union nestePeriode
            akkumulatorListe.removeLast()
            akkumulatorListe.add(oppdatertGjeldendePeriode)
        } else {
            akkumulatorListe.add(nestePeriode)
        }
        akkumulatorListe
    }
}

private fun erPerioderSammenhengende(gjeldendePeriode: Månedsperiode, nestePeriode: Månedsperiode) =
    gjeldendePeriode påfølgesAv nestePeriode
