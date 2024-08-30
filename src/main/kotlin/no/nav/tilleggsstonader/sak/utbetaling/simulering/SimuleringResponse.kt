package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDetaljer
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto

data class SimuleringResponse(val oppsummeringer: List<OppsummeringForPeriode>, val detaljer: SimuleringDetaljer)

object SimuleringResponseMapper {

    fun map(simuleringResponse: SimuleringResponseDto?): SimuleringResponse? {
        return simuleringResponse?.let {
            SimuleringResponse(
                oppsummeringer = it.oppsummeringer,
                detaljer = it.detaljer,
            )
        }
    }
}
