package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtbetalingV3Mapper(
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun lagUtbetalingRecords(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        totrinnskontroll: Totrinnskontroll?,
        erFørsteIverksettingForBehandling: Boolean,
        erSimulering: Boolean,
    ): List<UtbetalingRecord> {
        // En utbetaling er knyttet til en type andel (klassekode hos økonomi)
        val records =
            andelerTilkjentYtelse
                .groupBy { it.type }
                .map { (type, andelerTilkjentYtelseGruppertPåType) ->
                    val utbetalingId = fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(behandling.fagsakId, type)
                    lagUtbetalingRecord(
                        id = utbetalingId.utbetalingId,
                        erSimulering = erSimulering,
                        andelerTilkjentYtelse = andelerTilkjentYtelseGruppertPåType,
                        totrinnskontroll = totrinnskontroll,
                        behandling = behandling,
                        typeAndel = type,
                    )
                }

        return if (erFørsteIverksettingForBehandling) {
            records +
                lagUtbetalingRecordForAnnullering(
                    behandling = behandling,
                    andelerTilkjentYtelse = andelerTilkjentYtelse,
                    totrinnskontroll = totrinnskontroll,
                    erSimulering = erSimulering,
                )
        } else {
            records
        }
    }

    /**
     * Per nå tilsvarer [id] iverksettingId, men vi må se på hva vi ønsker med denne parameteren.
     */
    private fun lagUtbetalingRecord(
        id: UUID,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        totrinnskontroll: Totrinnskontroll?,
        behandling: Saksbehandling,
        typeAndel: TypeAndel,
        erSimulering: Boolean,
    ): UtbetalingRecord =
        UtbetalingRecord(
            id = id,
            dryrun = erSimulering,
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            saksbehandler = totrinnskontroll?.saksbehandler,
            beslutter = totrinnskontroll?.beslutter,
            vedtakstidspunkt = behandling.vedtakstidspunkt,
            periodetype = PeriodetypeUtbetaling.EN_GANG,
            perioder = mapPerioder(andelerTilkjentYtelse),
            stønad = mapTilStønadUtbetaling(typeAndel),
        )

    private fun mapPerioder(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<PerioderUtbetaling> =
        andelerTilkjentYtelse
            .filter { it.beløp != 0 }
            .groupBy { it.utbetalingsdato.toYearMonth() }
            .map { (månedÅr, andeler) ->
                PerioderUtbetaling(
                    fom = månedÅr.atDay(1),
                    tom = månedÅr.atEndOfMonth(),
                    beløp = andeler.sumOf { it.beløp }.toUInt(),
                )
            }

    private fun mapTilStønadUtbetaling(typeAndel: TypeAndel): StønadUtbetaling =
        when (typeAndel) {
            TypeAndel.DAGLIG_REISE_AAP -> StønadUtbetaling.DAGLIG_REISE_AAP
            TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER -> StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER
            TypeAndel.DAGLIG_REISE_ETTERLATTE -> StønadUtbetaling.DAGLIG_REISE_ETTERLATTE

            else -> error("Skal ikke sende andelstype=$typeAndel på kafka")
        }

    private fun lagUtbetalingRecordForAnnullering(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        totrinnskontroll: Totrinnskontroll?,
        erSimulering: Boolean,
    ): Collection<UtbetalingRecord> {
        val typeandelerSomSkalAnnulleres = finnTypeAndelerSomSkalAnnulleres(behandling, andelerTilkjentYtelse)
        val utbetalingIder =
            typeandelerSomSkalAnnulleres
                .map { fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(behandling.fagsakId, it) }

        return utbetalingIder.map {
            UtbetalingRecord(
                id = it.utbetalingId,
                dryrun = erSimulering,
                sakId = behandling.eksternFagsakId.toString(),
                behandlingId = behandling.eksternId.toString(),
                personident = behandling.ident,
                saksbehandler = totrinnskontroll?.saksbehandler,
                beslutter = totrinnskontroll?.beslutter,
                vedtakstidspunkt = behandling.vedtakstidspunkt,
                periodetype = PeriodetypeUtbetaling.EN_GANG,
                perioder = emptyList(),
                stønad = mapTilStønadUtbetaling(typeAndel = it.typeAndel),
            )
        }
    }

    private fun finnTypeAndelerSomSkalAnnulleres(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): List<TypeAndel> {
        if (behandling.forrigeIverksatteBehandlingId == null) {
            return emptyList()
        }
        val andelerForrigeBehandling =
            tilkjentYtelseService
                .hentForBehandling(behandling.forrigeIverksatteBehandlingId)
                .andelerTilkjentYtelse

        val typeAndelerNåværendeBehandling = andelerTilkjentYtelse.map { it.type }
        return andelerForrigeBehandling.filter { it.type !in typeAndelerNåværendeBehandling }.map { it.type }
    }
}
