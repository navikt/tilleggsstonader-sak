package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.UtbetalingFagområde
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtbetalingStatusHåndterer(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    // I første omgang vil vi kun plukke opp status for nye fagområder, da disse har gått over kafka
    // Ved migrering av gamle stønader til nye fagområder må vi også lytte på disse her
    private val nyeFagområderTilleggsstønader =
        UtbetalingFagområde.entries
            .filter { it.erNyeFagområder() }
            .map { it.kode }

    /*
     Iverksetting over v2/rest vil også komme inn her. Da vil key fra topic ikke være iverksettingId,
     men en id generert av helved som vi ikke har kjennskap til
     */
    fun behandleStatusoppdatering(
        iverksettingId: String,
        melding: UtbetalingStatusRecord,
    ) {
        if (melding.detaljer?.ytelse !in nyeFagområderTilleggsstønader) {
            return
        }
        val utbetalingsstatus = melding.status
        val andeler = andelTilkjentYtelseRepository.findByIverksettingIverksettingId(UUID.fromString(iverksettingId))

        if (andeler.isNotEmpty()) {
            logger.info(
                "Mottatt utbetalingsstatus=${utbetalingsstatus.name} for iverksettingId=$iverksettingId. Oppdaterer${andeler.size} andel(er)",
            )
            sanityCheckStatusPåAndelerKnyttetTilIverksetting(andeler, iverksettingId)
            andelTilkjentYtelseRepository.updateAll(andeler.map { it.copy(statusIverksetting = utbetalingsstatus.tilStatusIverksetting()) })
        }
    }
}

fun UtbetalingStatus.tilStatusIverksetting(): StatusIverksetting =
    when (this) {
        UtbetalingStatus.OK -> StatusIverksetting.OK
        UtbetalingStatus.FEILET -> StatusIverksetting.FEILET
        UtbetalingStatus.MOTTATT -> StatusIverksetting.MOTTATT
        UtbetalingStatus.HOS_OPPDRAG -> StatusIverksetting.HOS_OPPDRAG
    }

// Statuser som tilsier at en andel aldri skal ha vært iverksatt
private val uforventedeStatuser =
    listOf(
        StatusIverksetting.UBEHANDLET,
        StatusIverksetting.VENTER_PÅ_SATS_ENDRING,
        StatusIverksetting.UAKTUELL,
    )

private fun sanityCheckStatusPåAndelerKnyttetTilIverksetting(
    andeler: List<AndelTilkjentYtelse>,
    iverksettingId: String,
) {
    feilHvis(andeler.any { it.statusIverksetting in uforventedeStatuser }) {
        "Det finnes andeler på iverksetting=$iverksettingId som har en uforventet status"
    }
}
