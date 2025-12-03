package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UtbetalingV3Mapper(
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun lagSimuleringDtoer(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): Collection<SimuleringDto> =
        lagUtbetalinger(
            behandling = behandling,
            andeler = andelerTilkjentYtelse,
            erFørsteIverksetting = true,
        ) { utbetalingsgrunnlag -> SimuleringDto(utbetalingsgrunnlag) }

    fun lagIverksettingDtoer(
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
        ) { utbetalingsgrunnlag ->
            IverksettingDto(
                utbetalingsgrunnlag = utbetalingsgrunnlag,
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
        return UtbetalingGrunnlagDto(
            id = utbetalingId.utbetalingId,
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            periodetype = mapPeriodetypeFraAndeler(andeler),
            stønad = mapTilStønadUtbetaling(type),
            perioder = grupperPåMånedOgMapTilPerioder(andeler),
        )
    }

    fun mapPeriodetypeFraAndeler(andeler: Collection<AndelTilkjentYtelse>): PeriodetypeUtbetaling {
        val satstyper = andeler.distinctBy { it.satstype }
        feilHvis(satstyper.size != 1) {
            "Håndterer ikke andeler med flere ulike satstyper samtidig"
        }
        return satstyper.single().satstype.tilPeriodetypeUtbetaling()
    }

    fun Satstype.tilPeriodetypeUtbetaling() =
        when (this) {
            Satstype.DAG -> PeriodetypeUtbetaling.UKEDAG
            Satstype.MÅNED -> PeriodetypeUtbetaling.MND
            Satstype.ENGANGSBELØP -> PeriodetypeUtbetaling.EN_GANG
            Satstype.UGYLDIG -> error("Andeler med satstype UGYLDIG skal ikke iverksettes")
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
                    utbetalinger + lagUtbetalingDtoForAnnulering(behandling, andeler, utbetalingDtoFactory)
                } else {
                    utbetalinger
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

    private fun grupperPåMånedOgMapTilPerioder(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<PerioderUtbetaling> =
        andelerTilkjentYtelse
            .filter { it.beløp != 0 }
            .groupBy { it.utbetalingsdato }
            .map { (utbetalingsdato, andeler) ->
                PerioderUtbetaling(
                    fom = utbetalingsdato,
                    tom = utbetalingsdato,
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
