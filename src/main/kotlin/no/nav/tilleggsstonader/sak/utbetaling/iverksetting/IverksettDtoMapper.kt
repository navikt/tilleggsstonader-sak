package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import java.util.UUID

object IverksettDtoMapper {
    fun map(
        behandling: Saksbehandling,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        totrinnskontroll: Totrinnskontroll,
        iverksettingId: UUID,
        forrigeIverksetting: ForrigeIverksettingDto?,
    ): IverksettDto {
        feilHvisIkke(andelerTilkjentYtelse.any { it.iverksetting?.iverksettingId == iverksettingId }) {
            "Må inneholde en andel med iverksettingId=$iverksettingId"
        }
        return IverksettDto(
            sakId = behandling.eksternFagsakId.toString(),
            behandlingId = behandling.eksternId.toString(),
            iverksettingId = iverksettingId,
            personident = behandling.ident,
            forrigeIverksetting = forrigeIverksetting,
            vedtak = mapVedtak(behandling, totrinnskontroll, andelerTilkjentYtelse),
        )
    }

    private fun mapVedtak(
        behandling: Saksbehandling,
        totrinnskontroll: Totrinnskontroll,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): VedtaksdetaljerDto =
        VedtaksdetaljerDto(
            vedtakstidspunkt =
                behandling.vedtakstidspunkt
                    ?: error("Mangler vedtakstidspunkt behandling=${behandling.id}"),
            saksbehandlerId = totrinnskontroll.saksbehandler,
            beslutterId = totrinnskontroll.beslutter ?: error("Mangler beslutter"),
            utbetalinger = mapUtbetalinger(andelerTilkjentYtelse).sortedBy { it.fraOgMedDato },
        )

    fun mapUtbetalinger(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>) =
        andelerTilkjentYtelse
            .filter { it.beløp != 0 }
            .map {
                UtbetalingDto(
                    beløp = it.beløp,
                    satstype = it.satstype.tilSatstype(),
                    fraOgMedDato = it.fom,
                    tilOgMedDato = it.tom,
                    stønadsdata =
                        StønadsdataDto(
                            stønadstype = it.type.tilStønadstype(),
                            brukersNavKontor = null, // TODO denne skal settes for TSR-stønader (DAGLIG_REISE_TSR, etc),
                        ),
                )
            }

    private fun Satstype.tilSatstype(): SatstypeIverksetting =
        when (this) {
            Satstype.DAG -> SatstypeIverksetting.DAGLIG
            Satstype.MÅNED -> SatstypeIverksetting.MÅNEDLIG
            Satstype.ENGANGSBELØP -> SatstypeIverksetting.ENGANGS
            Satstype.UGYLDIG -> error("Ugyldig satstype. Skal ikke iverksettes")
        }

    fun TypeAndel.tilStønadstype(): StønadstypeIverksetting =
        when (this) {
            TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> StønadstypeIverksetting.TILSYN_BARN_ENSLIG_FORSØRGER
            TypeAndel.TILSYN_BARN_AAP -> StønadstypeIverksetting.TILSYN_BARN_AAP
            TypeAndel.TILSYN_BARN_ETTERLATTE -> StønadstypeIverksetting.TILSYN_BARN_ETTERLATTE

            TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER -> StønadstypeIverksetting.LÆREMIDLER_ENSLIG_FORSØRGER
            TypeAndel.LÆREMIDLER_AAP -> StønadstypeIverksetting.LÆREMIDLER_AAP
            TypeAndel.LÆREMIDLER_ETTERLATTE -> StønadstypeIverksetting.LÆREMIDLER_ETTERLATTE

            TypeAndel.BOUTGIFTER_AAP -> StønadstypeIverksetting.BOUTGIFTER_AAP
            TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER -> StønadstypeIverksetting.BOUTGIFTER_ENSLIG_FORSØRGER
            TypeAndel.BOUTGIFTER_ETTERLATTE -> StønadstypeIverksetting.BOUTGIFTER_ETTERLATTE

            TypeAndel.DAGLIG_REISE_AAP,
            TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER,
            TypeAndel.DAGLIG_REISE_ETTERLATTE,
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE,
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB,
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING,
            TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING,
            TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB,
            TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO,
            TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
            TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET,
            TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO,
            TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
            TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING,
            TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE,
            TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG,
            TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB,
            TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING,
            TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV,
            TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
            -> error("Andeler for daglig reise skal sendes på Kafka")

            TypeAndel.UGYLDIG -> error("Ugyldig type andel. Skal ikke iverksettes")
        }
}
