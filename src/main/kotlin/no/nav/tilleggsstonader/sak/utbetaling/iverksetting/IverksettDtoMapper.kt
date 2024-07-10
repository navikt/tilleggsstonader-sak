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
    ): VedtaksdetaljerDto {
        return VedtaksdetaljerDto(
            vedtakstidspunkt = behandling.vedtakstidspunkt
                ?: error("Mangler vedtakstidspunkt behandling=${behandling.id}"),
            saksbehandlerId = totrinnskontroll.saksbehandler,
            beslutterId = totrinnskontroll.beslutter ?: error("Mangler beslutter"),
            utbetalinger = mapUtbetalinger(andelerTilkjentYtelse),
        )
    }

    fun mapUtbetalinger(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>) =
        andelerTilkjentYtelse
            .filter { it.beløp != 0 }
            .map {
                UtbetalingDto(
                    beløp = it.beløp,
                    satstype = it.satstype.tilSatstype(),
                    fraOgMedDato = it.fom,
                    tilOgMedDato = it.tom,
                    stønadstype = it.type.tilStønadstype(),
                    brukersNavKontor = null, // TODO denne skal settes for reise?
                )
            }

    private fun Satstype.tilSatstype(): SatstypeIverksetting = when (this) {
        Satstype.DAG -> SatstypeIverksetting.DAGLIG
        Satstype.MÅNED -> SatstypeIverksetting.MÅNEDLIG
        Satstype.ENGANGSBELØP -> SatstypeIverksetting.ENGANGS
        Satstype.UGYLDIG -> error("Ugyldig satstype. Skal ikke iverksettes")
    }

    private fun TypeAndel.tilStønadstype(): StønadstypeIverksetting = when (this) {
        TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> StønadstypeIverksetting.TILSYN_BARN_ENSLIG_FORSØRGER
        TypeAndel.TILSYN_BARN_AAP -> StønadstypeIverksetting.TILSYN_BARN_AAP
        TypeAndel.TILSYN_BARN_ETTERLATTE -> StønadstypeIverksetting.TILSYN_BARN_ETTERLATTE
        TypeAndel.UGYLDIG -> error("Ugyldig type andel. Skal ikke iverksettes")
    }
}
