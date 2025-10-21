package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtbetalingStatusHåndterer(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    fun behandleStatusoppdatering(
        iverksettingId: String,
        melding: UtbetalingStatusRecord,
    ) {
        if (melding.detaljer.ytelse != "TILLEGGSSTØNADER") {
            return
        }
        val utbetalingsstatus = melding.status
        val andeler = andelTilkjentYtelseRepository.findByIverksettingId(UUID.fromString(iverksettingId))

        sanityCheckAndelene(andeler, iverksettingId)

        andelTilkjentYtelseRepository.updateAll(andeler.map { it.copy(statusIverksetting = utbetalingsstatus.tilStatusIverksetting()) })
    }
}

fun UtbetalingStatus.tilStatusIverksetting(): StatusIverksetting =
    when (this) {
        UtbetalingStatus.OK -> StatusIverksetting.OK
        UtbetalingStatus.FEILET -> StatusIverksetting.FEILET
        UtbetalingStatus.MOTTATT -> StatusIverksetting.MOTTATT
        UtbetalingStatus.HOS_OPPDRAG -> StatusIverksetting.HOS_OPPDRAG
    }

private fun sanityCheckAndelene(
    andeler: List<AndelTilkjentYtelse>,
    iverksettingId: String,
) {
    feilHvis(andeler.any { it.statusIverksetting != StatusIverksetting.SENDT }) {
        "Finnes andeler på iverksetting=$iverksettingId som har annen status enn ${StatusIverksetting.SENDT}"
    }
    feilHvis(andeler.isEmpty()) {
        "Forventet å finne minimum en andel for iverksetting=$iverksettingId"
    }
}
