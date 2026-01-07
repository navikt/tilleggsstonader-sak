package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class UtbetalingV3Mapper(
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun lagSimuleringDtoer(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): SimuleringDto =
        SimuleringDto(
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            periodetype = mapPeriodetypeFraAndeler(andelerTilkjentYtelse),
            utbetalinger = lagUtbetalinger(behandling, andelerTilkjentYtelse, erFørsteIverksettingForBehandling = true),
        )

    fun lagIverksettingDtoer(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        erFørsteIverksettingForBehandling: Boolean,
        totrinnskontroll: Totrinnskontroll?,
        vedtakstidspunkt: LocalDateTime,
    ): IverksettingDto =
        IverksettingDto(
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            periodetype = mapPeriodetypeFraAndeler(andelerTilkjentYtelse),
            utbetalinger = lagUtbetalinger(behandling, andelerTilkjentYtelse, erFørsteIverksettingForBehandling),
            saksbehandler = totrinnskontroll?.saksbehandler ?: error("Saksbehandler mangler"),
            beslutter = totrinnskontroll.beslutter ?: error("Beslutter mangler"),
            vedtakstidspunkt = vedtakstidspunkt,
        )

    private fun lagUtbetalinger(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        erFørsteIverksettingForBehandling: Boolean,
    ): List<Utbetaling> =
        andelerTilkjentYtelse
            .groupBy { it.type }
            .map { (type, andelerAvType) -> lagUtbetaling(behandling, type, andelerAvType) }
            .let { utbetalinger ->
                if (erFørsteIverksettingForBehandling) {
                    utbetalinger + lagUtbetalingDtoForAnnulering(behandling, andelerTilkjentYtelse)
                } else {
                    utbetalinger
                }
            }

    private fun lagUtbetaling(
        behandling: Saksbehandling,
        type: TypeAndel,
        andeler: Collection<AndelTilkjentYtelse>,
    ): Utbetaling {
        val utbetalingId = fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(behandling.fagsakId, type)
        return Utbetaling(
            id = utbetalingId.utbetalingId,
            stønad = mapTilStønadUtbetaling(type),
            perioder = grupperPåDagOgMapTilPerioder(andeler),
            brukFagområdeTillst = false, // TODO - må settes til true for saker som har tatt i bruk dette fagområdet
        )
    }

    fun mapPeriodetypeFraAndeler(andeler: Collection<AndelTilkjentYtelse>): PeriodetypeUtbetaling {
        val satstyper = andeler.distinctBy { it.satstype }
        println(satstyper)
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

    private fun lagUtbetalingDtoForAnnulering(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): List<Utbetaling> =
        finnTypeAndelerSomSkalAnnulleres(behandling, andelerTilkjentYtelse)
            .map { typeAndel ->
                lagUtbetaling(
                    behandling = behandling,
                    type = typeAndel,
                    andeler = emptyList(), // periodene skal annuleres 💥
                )
            }

    private fun grupperPåDagOgMapTilPerioder(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<UtbetalingPeriodeDto> =
        andelerTilkjentYtelse
            .filter { it.beløp != 0 }
            .groupBy { it.utbetalingsdato }
            .map { (utbetalingsdato, andeler) ->
                UtbetalingPeriodeDto(
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
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE -> StønadUtbetaling.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB -> StønadUtbetaling.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING -> StønadUtbetaling.DAGLIG_REISE_TILTAK_ARBEIDSTRENING
            TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING -> StønadUtbetaling.DAGLIG_REISE_TILTAK_AVKLARING
            TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB -> StønadUtbetaling.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB
            TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO -> StønadUtbetaling.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO
            TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD ->
                StønadUtbetaling.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD
            TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET ->
                StønadUtbetaling.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET
            TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO -> StønadUtbetaling.DAGLIG_REISE_TILTAK_GRUPPE_AMO
            TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD -> StønadUtbetaling.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD
            TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING -> StønadUtbetaling.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING
            TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE -> StønadUtbetaling.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE
            TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG -> StønadUtbetaling.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG
            TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB -> StønadUtbetaling.DAGLIG_REISE_TILTAK_JOBBKLUBB
            TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING -> StønadUtbetaling.DAGLIG_REISE_TILTAK_OPPFØLGING
            TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV -> StønadUtbetaling.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV
            TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING ->
                StønadUtbetaling.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING
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
