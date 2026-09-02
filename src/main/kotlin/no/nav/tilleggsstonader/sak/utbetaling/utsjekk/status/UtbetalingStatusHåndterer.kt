package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status

import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtbetalingStatusHåndterer(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    /*
     Iverksetting over v2/rest vil også komme inn her. Da vil key fra topic ikke være iverksettingId,
     men en id generert av helved som vi ikke har kjennskap til
     */
    fun behandleStatusoppdatering(
        iverksettingId: String,
        melding: UtbetalingStatusRecord,
        utbetalingGjelderFagsystem: String,
    ) {
        if (utbetalingGjelderFagsystem != FAGSYSTEM_TILLEGGSSTØNADER) {
            return
        }

        logger.info(
            "Mottak utbetaling-status for fagområde ${melding.detaljer?.ytelse}, med iverksettingId=$iverksettingId og status=${melding.status}",
        )

        val utbetalingsstatus = melding.status

        val andeler = andelTilkjentYtelseRepository.findByIverksettingIverksettingId(UUID.fromString(iverksettingId))

        if (andeler.isNotEmpty()) {
            loggStatusendringPåIverksetting(iverksettingId, utbetalingsstatus, andeler.size)

            feilHvis(andelerHarUforventetStatus(andeler)) {
                "Det finnes andeler på iverksetting=$iverksettingId som har en uforventet status"
            }

            if (skalOppdatereStatus(andeler, utbetalingsstatus.tilStatusIverksetting())) {
                andelTilkjentYtelseRepository.updateAll(
                    andeler.map {
                        it.copy(statusIverksetting = utbetalingsstatus.tilStatusIverksetting())
                    },
                )
            }
        } else {
            logger.warn(
                "Mottatt feilet status for iverksettingId=$iverksettingId som ikke refererer noen andel. Gjelder sannsynligvis simulering. BehandlingIder: ${melding.detaljer?.alleBehandlingIder()}",
            )
        }
    }

    private fun loggStatusendringPåIverksetting(
        iverksettingId: String,
        utbetalingStatus: UtbetalingStatus,
        antallAndelerSomOppdateres: Int,
    ) {
        if (utbetalingStatus == UtbetalingStatus.FEILET) {
            logger.error(
                "Mottatt feilet utbetaling med status=${utbetalingStatus.name} for iverksettingId=$iverksettingId. Gjelder $antallAndelerSomOppdateres andel(er)",
            )
        } else {
            logger.info(
                "Mottatt utbetalingsstatus=${utbetalingStatus.name} for iverksettingId=$iverksettingId. Gjelder $antallAndelerSomOppdateres andel(er)",
            )
        }
    }

    // HOS_OPPDRAG kommer noen ganger etter vi har mottatt OK, unngår da å endre tilbake til HOS_OPPDRAG, da det ikke vil komme enda en OK-status etter
    private fun skalOppdatereStatus(
        andeler: List<AndelTilkjentYtelse>,
        nyStatusIverksetting: StatusIverksetting,
    ): Boolean = !(andeler.all { it.statusIverksetting == StatusIverksetting.OK } && nyStatusIverksetting == StatusIverksetting.HOS_OPPDRAG)

    companion object {
        const val FAGSYSTEM_TILLEGGSSTØNADER = "TILLEGGSSTØNADER"
    }
}

fun UtbetalingStatus.tilStatusIverksetting(): StatusIverksetting =
    when (this) {
        UtbetalingStatus.OK -> StatusIverksetting.OK
        UtbetalingStatus.FEILET -> StatusIverksetting.FEILET
        UtbetalingStatus.MOTTATT -> StatusIverksetting.MOTTATT
        UtbetalingStatus.HOS_OPPDRAG -> StatusIverksetting.HOS_OPPDRAG
    }

private fun andelerHarUforventetStatus(andeler: List<AndelTilkjentYtelse>): Boolean {
    // Statuser som tilsier at en andel aldri skal ha vært iverksatt
    val uforventedeStatuser =
        listOf(
            StatusIverksetting.UBEHANDLET,
            StatusIverksetting.VENTER_PÅ_SATS_ENDRING,
            StatusIverksetting.UAKTUELL,
        )

    return andeler.any { it.statusIverksetting in uforventedeStatuser }
}
