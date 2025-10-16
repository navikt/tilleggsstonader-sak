package no.nav.tilleggsstonader.sak.utbetaling.utsjekk

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.ForrigeIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import java.util.UUID

object UtbetalingMapper {
    /**
     * Per nå tilsvarer [id] iverksettingId, men vi må se på hva vi ønsker med denne parameteren.
     */
    fun lagUtbetalingRecord(
        id: UUID,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        totrinnskontroll: Totrinnskontroll,
        behandling: Saksbehandling,
        forrigeUtbetaling: ForrigeIverksetting?,
    ): UtbetalingRecord =
        UtbetalingRecord(
            id = id,
            forrigeUtbetaling = forrigeUtbetaling?.let { ForrigeUtbetaling(it.iverksettingId, it.behandlingId) },
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            stønad = mapTilStønadUtbetaling(andelerTilkjentYtelse),
            saksbehandler = totrinnskontroll.saksbehandler,
            beslutter = totrinnskontroll.beslutter ?: error("Mangler beslutter behandling=${behandling.id}"),
            vedtakstidspunkt = behandling.vedtakstidspunkt ?: error("Mangler vedtakstidspunkt behandling=${behandling.id}"),
            periodetype = PeriodetypeUtbetaling.EN_GANG,
            perioder = mapPerioder(andelerTilkjentYtelse),
        )

    private fun mapPerioder(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<PerioderUtbetaling> =
        andelerTilkjentYtelse
            .filter { it.beløp != 0 }
            .map {
                PerioderUtbetaling(
                    fom = it.fom,
                    tom = it.tom,
                    beløp = it.beløp.toUInt(),
                )
            }

    private fun mapTilStønadUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): StønadUtbetaling {
        feilHvis(andelerTilkjentYtelse.distinctBy { it.type }.count() != 1) {
            "Forventer én og bare én type andel i utbetalingen"
        }
        return when (val andelstype = andelerTilkjentYtelse.first().type) {
            TypeAndel.DAGLIG_REISE_AAP -> StønadUtbetaling.DAGLIG_REISE_AAP
            TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER -> StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER
            TypeAndel.DAGLIG_REISE_ETTERLATTE -> StønadUtbetaling.DAGLIG_REISE_ETTERLATTE

            else -> error("Skal ikke sende andelstype=$andelstype på kafka")
        }
    }
}
