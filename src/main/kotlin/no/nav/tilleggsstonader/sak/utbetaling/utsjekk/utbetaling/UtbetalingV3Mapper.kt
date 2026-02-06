package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class UtbetalingV3Mapper(
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    fun lagSimuleringDtoer(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): SimuleringDto {
        validerAndeler(andelerTilkjentYtelse)
        return SimuleringDto(
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            periodetype = PeriodetypeUtbetaling.UKEDAG,
            utbetalinger = lagUtbetalinger(behandling, andelerTilkjentYtelse, erFørsteIverksettingForBehandling = true),
        )
    }

    fun lagIverksettingDtoer(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        erFørsteIverksettingForBehandling: Boolean,
        totrinnskontroll: Totrinnskontroll?,
        vedtakstidspunkt: LocalDateTime,
    ): IverksettingDto {
        validerAndeler(andelerTilkjentYtelse)
        return IverksettingDto(
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            personident = behandling.ident,
            periodetype = PeriodetypeUtbetaling.UKEDAG,
            utbetalinger = lagUtbetalinger(behandling, andelerTilkjentYtelse, erFørsteIverksettingForBehandling),
            saksbehandler = totrinnskontroll?.saksbehandler ?: error("Saksbehandler mangler"),
            beslutter = totrinnskontroll.beslutter ?: error("Beslutter mangler"),
            vedtakstidspunkt = vedtakstidspunkt,
        )
    }

    private fun validerAndeler(andeler: Collection<AndelTilkjentYtelse>) {
        feilHvis(andeler.distinctBy { it.satstype }.size > 1) {
            "Håndterer ikke andeler med flere ulike satstyper samtidig"
        }
    }

    private fun lagUtbetalinger(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        erFørsteIverksettingForBehandling: Boolean,
    ): List<UtbetalingDto> =
        andelerTilkjentYtelse
            .filterNot { it.erNullandel() }
            .groupBy { it.type }
            .map { (type, andelerAvType) -> lagUtbetaling(behandling, type, andelerAvType) }
            .let { utbetalinger ->
                if (erFørsteIverksettingForBehandling) {
                    utbetalinger + lagUtbetalingDtoForAnnulering(behandling, andelerTilkjentYtelse)
                } else {
                    utbetalinger
                }
            }

    private fun Stønadstype.skalBrukeGamleFagområder() =
        Stønadstype.BARNETILSYN == this || Stønadstype.LÆREMIDLER == this || Stønadstype.BOUTGIFTER == this

    private fun lagUtbetaling(
        behandling: Saksbehandling,
        type: TypeAndel,
        andeler: Collection<AndelTilkjentYtelse>,
    ): UtbetalingDto {
        val utbetalingId = fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(behandling.fagsakId, type)
        return UtbetalingDto(
            id = utbetalingId.utbetalingId,
            stønad = mapTilStønadUtbetaling(type),
            perioder = grupperPåDagOgMapTilPerioder(andeler),
            brukFagområdeTillst = behandling.stønadstype.skalBrukeGamleFagområder(),
        )
    }

    private fun lagUtbetalingDtoForAnnulering(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): List<UtbetalingDto> =
        finnTypeAndelerSomSkalAnnulleres(behandling, andelerTilkjentYtelse)
            .map { typeAndel ->
                lagUtbetaling(
                    behandling = behandling,
                    type = typeAndel,
                    andeler = emptyList(), // periodene skal annuleres 💥
                )
            }

    // Grupperer alle andeler som har samme fom. NB: hvis vi tar i bruk noe annet enn dagsats må dette endres, da tom kan være ulik
    private fun grupperPåDagOgMapTilPerioder(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<UtbetalingPeriodeDto> =
        andelerTilkjentYtelse
            .filter { it.beløp != 0 }
            .groupBy { UtbetalingsDatoOgEnhet(it.fom, it.brukersNavKontor) }
            .map { (utbetalingsdatoOgEnhet, andeler) ->
                UtbetalingPeriodeDto(
                    fom = utbetalingsdatoOgEnhet.utbetalingsperiodeFom,
                    tom = utbetalingsdatoOgEnhet.utbetalingsperiodeFom,
                    beløp = andeler.sumOf { it.beløp }.toUInt(),
                    betalendeEnhet = utbetalingsdatoOgEnhet.betalendeEnhet,
                )
            }

    // Hjelpeklasse for å gruppere andeler på utbetalingsdato og betalende enhet
    private data class UtbetalingsDatoOgEnhet(
        val utbetalingsperiodeFom: LocalDate,
        val betalendeEnhet: String?,
    )

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

            TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER -> StønadUtbetaling.LÆREMIDLER_ENSLIG_FORSØRGER
            TypeAndel.LÆREMIDLER_AAP -> StønadUtbetaling.LÆREMIDLER_AAP
            TypeAndel.LÆREMIDLER_ETTERLATTE -> StønadUtbetaling.LÆREMIDLER_ETTERLATTE

            TypeAndel.BOUTGIFTER_AAP -> StønadUtbetaling.BOUTGIFTER_AAP
            TypeAndel.BOUTGIFTER_ETTERLATTE -> StønadUtbetaling.BOUTGIFTER_ETTERLATTE
            TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER -> StønadUtbetaling.BOUTGIFTER_ENSLIG_FORSØRGER

            TypeAndel.TILSYN_BARN_AAP -> StønadUtbetaling.TILSYN_BARN_AAP
            TypeAndel.TILSYN_BARN_ETTERLATTE -> StønadUtbetaling.TILSYN_BARN_ETTERLATTE
            TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> StønadUtbetaling.TILSYN_BARN_ENSLIG_FORSØRGER

            else -> error("Skal ikke sende andelstype=$typeAndel på kafka")
        }

    private fun finnTypeAndelerSomSkalAnnulleres(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): Set<TypeAndel> {
        if (behandling.forrigeIverksatteBehandlingId == null) {
            return emptySet()
        }
        val andelerForrigeBehandling =
            tilkjentYtelseService
                .hentForBehandling(behandling.forrigeIverksatteBehandlingId)
                .andelerTilkjentYtelse

        val typeAndelerNåværendeBehandling = andelerTilkjentYtelse.map { it.type }
        return andelerForrigeBehandling
            .filter { it.type !in typeAndelerNåværendeBehandling }
            .map { it.type }
            .filter { it != TypeAndel.UGYLDIG }
            .toSet()
    }
}
