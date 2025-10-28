package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Per nå tilsvarer [id] iverksettingId, men vi må se på hva vi ønsker med denne parameteren.
 */
@Service
class UtbetalingV3Mapper(
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun lagSimuleringDto(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): Collection<SimuleringDto> =
        lagUtbetalinger(
            behandling = behandling,
            andeler = andelerTilkjentYtelse,
            erFørsteIverksetting = true,
        ) { grunnlag -> SimuleringDto(grunnlag = grunnlag) }

    fun lagIverksettingRecord(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        erFørsteIverksettingForBehandling: Boolean,
        totrinnskontroll: Totrinnskontroll?,
        vedtakstidspunkt: LocalDateTime,
    ): Collection<IverksettingDto> =
        lagUtbetalinger(
            behandling = behandling,
            andeler = andelerTilkjentYtelse,
            erFørsteIverksetting = erFørsteIverksettingForBehandling,
        ) { grunnlag ->
            IverksettingDto(
                grunnlag = grunnlag,
                saksbehandler = totrinnskontroll?.saksbehandler ?: error("Saksbehandler mangler"),
                beslutter = totrinnskontroll.beslutter ?: error("Beslutter mangler"),
                vedtakstidspunkt = vedtakstidspunkt,
            )
        }

    private fun lagUtbetalingGrunnlag(
        behandling: Saksbehandling,
        type: TypeAndel,
        andeler: Collection<AndelTilkjentYtelse>,
    ): UtbetalingGrunnlagDto {
        val utbetalingId = fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(behandling.fagsakId, type)
        val andelerKrysserÅrsskiftet = andeler.distinctBy { it.utbetalingsdato.year }.size > 1
        brukerfeilHvis(andelerKrysserÅrsskiftet) { "Alle andeler i én og samme utbetaling må være innenfor samme år." }

        return UtbetalingGrunnlagDto(
            id = utbetalingId.utbetalingId,
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            periodetype = PeriodetypeUtbetaling.EN_GANG,
            stønad = mapTilStønadUtbetaling(typeAndel = type),
            perioder = mapPerioder(andelerTilkjentYtelse = andeler),
        )
    }

    private fun <T : UtbetalingDto> lagUtbetalinger(
        behandling: Saksbehandling,
        andeler: Collection<AndelTilkjentYtelse>,
        erFørsteIverksetting: Boolean,
        utbetalingDtoFactory: (UtbetalingGrunnlagDto) -> T,
    ): Collection<T> =
        andeler
            .groupBy { it.type }
            .map { (type, andelerAvType) -> utbetalingDtoFactory(lagUtbetalingGrunnlag(behandling, type, andelerAvType)) }
            .let { utbetalinger ->
                if (erFørsteIverksetting) {
                    utbetalinger
                } else {
                    utbetalinger + lagUtbetalingDtoForAnnulering(behandling, andeler, utbetalingDtoFactory)
                }
            }

    private fun <T : UtbetalingDto> lagUtbetalingDtoForAnnulering(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        utbetalingDtoFactory: (UtbetalingGrunnlagDto) -> T,
    ): Collection<T> =
        finnTypeAndelerSomSkalAnnulleres(behandling, andelerTilkjentYtelse)
            .map { typeAndel ->
                val grunnlag =
                    lagUtbetalingGrunnlag(
                        behandling = behandling,
                        type = typeAndel,
                        andeler = emptyList(), // periodene skal annuleres 💥
                    )
                utbetalingDtoFactory(grunnlag)
            }

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
