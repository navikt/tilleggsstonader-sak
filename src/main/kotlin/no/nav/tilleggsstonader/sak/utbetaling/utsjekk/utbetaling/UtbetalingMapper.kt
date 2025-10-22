package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
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
        typeAndel: TypeAndel,
    ): UtbetalingRecord =
        UtbetalingRecord(
            id = id,
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            saksbehandler = totrinnskontroll.saksbehandler,
            beslutter = totrinnskontroll.beslutter ?: error("Mangler beslutter behandling=${behandling.id}"),
            vedtakstidspunkt = behandling.vedtakstidspunkt ?: error("Mangler vedtakstidspunkt behandling=${behandling.id}"),
            periodetype = PeriodetypeUtbetaling.EN_GANG,
            perioder = mapPerioder(andelerTilkjentYtelse),
            stønad = mapTilStønadUtbetaling(typeAndel),
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

    private fun mapTilStønadUtbetaling(typeAndel: TypeAndel): StønadUtbetaling =
        when (typeAndel) {
            TypeAndel.DAGLIG_REISE_AAP -> StønadUtbetaling.DAGLIG_REISE_AAP
            TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER -> StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER
            TypeAndel.DAGLIG_REISE_ETTERLATTE -> StønadUtbetaling.DAGLIG_REISE_ETTERLATTE

            else -> error("Skal ikke sende andelstype=$typeAndel på kafka")
        }

    fun lagTomUtbetalingRecordForAnnullering(
        id: UUID,
        behandling: Saksbehandling,
        totrinnskontroll: Totrinnskontroll,
        typeAndel: TypeAndel,
    ) = UtbetalingRecord(
        id = id,
        sakId = behandling.eksternFagsakId.toString(),
        behandlingId = behandling.eksternId.toString(),
        personident = behandling.ident,
        saksbehandler = totrinnskontroll.saksbehandler,
        beslutter = totrinnskontroll.beslutter ?: error("Mangler beslutter behandling=${behandling.id}"),
        vedtakstidspunkt = behandling.vedtakstidspunkt ?: error("Mangler vedtakstidspunkt behandling=${behandling.id}"),
        periodetype = PeriodetypeUtbetaling.EN_GANG,
        perioder = emptyList(),
        stønad = mapTilStønadUtbetaling(typeAndel),
    )
}
